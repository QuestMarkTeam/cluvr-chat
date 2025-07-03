// 전역 변수들
let currentRoomId = null;
let currentRoomName = null;
let clubId = null;
let role = null;
let userInfo = null;
let stompClient = null;
let socket = null;
let currentSubscription = null;
let lastDisplayedDate = null;

// API 엔드포인트
const API_DOMAIN_URL = 'http://44.239.99.137:80';
const API_CHAT_URL = 'http://54.200.146.243';

// 쿼리스트링 파싱 함수
function getQueryParam(name) {
    const url = new URL(window.location.href);
    return url.searchParams.get(name);
}

// 토큰 관리 함수들
function saveToken(token) {
    sessionStorage.setItem('authToken', token);
}

function getToken() {
    return sessionStorage.getItem('authToken');
}

function clearToken() {
    sessionStorage.removeItem('authToken');
}

// 에러 처리 함수
function handleApiError(error, defaultMessage) {
    console.error('API Error:', error);
    
    if (error.response?.status === 401) {
        showNotification('인증이 만료되었습니다. 다시 로그인해주세요.', 'error');
        clearToken();
        setTimeout(() => {
            window.location.href = '/login.html';
        }, 2000);
    } else if (error.response?.status === 403) {
        showNotification('접근 권한이 없습니다.', 'error');
    } else if (error.response?.status === 404) {
        showNotification('요청한 리소스를 찾을 수 없습니다.', 'error');
    } else if (error.response?.status >= 500) {
        showNotification('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.', 'error');
    } else {
        showNotification(defaultMessage, 'error');
    }
}

// 페이지 로드 시 초기화
window.addEventListener('DOMContentLoaded', () => {
    const clubId = getQueryParam('clubId');
    const token = getQueryParam('token');

    if (clubId) document.getElementById('clubId').value = clubId;
    if (token) {
        saveToken(token);
        document.getElementById('token').value = token;
    }

    // clubId와 token이 있으면 자동으로 채팅방 리스트 불러오기
    if (clubId && token) fetchChatRooms();

    // 멤버 오버레이 토글 - 수정된 부분
    const openBtn = document.getElementById('openMembersBtn');
    const closeBtn = document.getElementById('closeMembersBtn');
    const overlay = document.getElementById('membersOverlay');

    openBtn.addEventListener('click', () => {
        overlay.style.display = 'block';
        setTimeout(() => overlay.classList.add('show'), 10);
    });

    closeBtn.addEventListener('click', () => {
        overlay.classList.remove('show');
        setTimeout(() => overlay.style.display = 'none', 300);
    });

    // ESC 키로 닫기
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            overlay.classList.remove('show');
            setTimeout(() => overlay.style.display = 'none', 300);
        }
    });

    // 엔터키로 메시지 전송
    document.getElementById("messageInput").addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            sendMessage();
        }
    });
});

// 채팅방 입장 함수
function joinChatRoom() {
    const token = getToken();
    console.log("✅ 토큰값:", token);

    return fetch(`${API_CHAT_URL}/api/clubs/${clubId}/chat/rooms/${currentRoomId}/join`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        }
    }).catch(error => {
        handleApiError(error, '채팅방 입장에 실패했습니다.');
    });
}

// 클럽 정보 업데이트
function updateClubInfo(clubName) {
    const clubInfoDiv = document.getElementById('clubInfo');
    const clubNameSpan = document.getElementById('clubName');

    if (clubName) {
        clubNameSpan.textContent = `🍀 ${clubName}`;
        clubInfoDiv.style.display = 'block';
    } else {
        clubInfoDiv.style.display = 'none';
    }
}

// 채팅방 멤버 목록 조회
async function fetchRoomMembers(roomId) {
    try {
        const token = getToken();
        const response = await fetch(`${API_CHAT_URL}/api/clubs/${clubId}/chat/rooms/${roomId}/users`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        
        if (response.ok) {
            const data = await response.json();
            updateMembersList(data.data || []);
        } else {
            throw new Error(`HTTP ${response.status}`);
        }
    } catch (error) {
        console.error('멤버 목록 조회 실패:', error);
        handleApiError(error, '멤버 목록을 불러올 수 없습니다.');
        updateMembersList([]);
    }
}

// 멤버 목록 업데이트
function updateMembersList(members) {
    const membersList = document.getElementById('membersList');
    const memberCount = document.getElementById('memberCount');

    memberCount.textContent = members.length;

    if (members.length === 0) {
        membersList.innerHTML = `
            <div style="text-align: center; color: #888; padding: 20px;">
                멤버 정보를 불러올 수 없습니다
            </div>
        `;
        return;
    }

    membersList.innerHTML = members.map(member => {
        const roleClass = `role-${member.clubRole.toLowerCase()}`;
        const initials = member.nickname ? member.nickname.charAt(0).toUpperCase() : 'U';

        return `
            <div class="member-item">
                <div class="member-avatar">${initials}</div>
                <div class="member-info">
                    <div class="member-name">${escapeHtml(member.nickname || '익명')}</div>
                    <div class="member-role">
                        <span class="role-badge ${roleClass}">${member.clubRole}</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// 소켓 연결 및 구독
function connectSocketAndSubscribe(roomId, roomName) {
    currentRoomId = roomId;
    currentRoomName = roomName;

    // 현재 채팅방 이름 업데이트
    document.getElementById('currentRoomName').textContent = roomName;

    // 입력 필드 활성화
    document.getElementById('messageInput').disabled = false;
    document.getElementById('sendBtn').disabled = false;

    // 이전 구독 해제
    if (currentSubscription) {
        currentSubscription.unsubscribe();
    }

    // 채팅창 비우기
    const chatBox = document.getElementById("chatBox");
    chatBox.innerHTML = "";
    lastDisplayedDate = null; // 날짜 초기화

    // 연결 상태에 따라 소켓 연결 관리
    if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {
            console.log("Disconnected previous STOMP connection.");
            startNewConnection();
        });
    } else {
        startNewConnection();
    }

    // 새로운 연결 시작
    function startNewConnection() {
        clubId = parseInt(document.getElementById("clubId").value);
        const token = getToken();
        socket = new SockJS(`${API_CHAT_URL}/ws/chat?token=${encodeURIComponent(token)}`);
        stompClient = Stomp.over(socket);

        stompClient.connect({}, () => {
            joinChatRoom().then(() => {
                currentSubscription = stompClient.subscribe(`/sub/ws/chat/rooms/${currentRoomId}`, function (message) {
                    const msgObj = JSON.parse(message.body);
                    renderMessage(msgObj);
                });

                // 채팅 기록 불러오기
                fetch(`${API_CHAT_URL}/api/clubs/${clubId}/chat/rooms/${currentRoomId}`, {
                    headers: {
                        "Authorization": `Bearer ${token}`
                    }
                })
                    .then(res => {
                        if (!res.ok) {
                            throw new Error(`HTTP ${res.status}`);
                        }
                        return res.json();
                    })
                    .then(data => {
                        const chatBox = document.getElementById("chatBox");
                        chatBox.innerHTML = "";
                        lastDisplayedDate = null;
                        if (data.data && Array.isArray(data.data)) {
                            data.data.forEach(renderMessage);
                        }
                    }).catch(error => {
                    handleApiError(error, '채팅 기록을 불러올 수 없습니다.');
                });

                // 멤버 목록 불러오기
                fetchRoomMembers(currentRoomId);
            });
        }, (error) => {
            console.error('WebSocket 연결 실패:', error);
            handleApiError(error, '실시간 연결에 실패했습니다.');
        });
    }
}

// HTML 이스케이프 함수
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 메시지 렌더링
function renderMessage(message) {
    const chatBox = document.getElementById("chatBox");

    // 날짜 처리
    if (message.createdAt) {
        const messageDate = new Date(message.createdAt);
        const today = new Date();
        const yesterday = new Date(today);
        yesterday.setDate(yesterday.getDate() - 1);

        const messageDateStr = messageDate.toDateString();
        const todayStr = today.toDateString();
        const yesterdayStr = yesterday.toDateString();

        let dateLabel;
        if (messageDateStr === todayStr) {
            dateLabel = "오늘";
        } else if (messageDateStr === yesterdayStr) {
            dateLabel = "어제";
        } else {
            dateLabel = messageDate.toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                weekday: 'long'
            });
        }

        // 날짜가 바뀌었을 때만 날짜 구분선 표시
        if (lastDisplayedDate !== messageDateStr) {
            const dateDiv = document.createElement("div");
            dateDiv.className = "date-divider";
            dateDiv.innerHTML = `<span class="date-text">${dateLabel}</span>`;
            chatBox.appendChild(dateDiv);
            lastDisplayedDate = messageDateStr;
        }
    }

    const msgDiv = document.createElement("div");

    if (message.type === "ENTER" || message.type === "LEAVE") {
        msgDiv.className = "chat system";
        msgDiv.textContent = escapeHtml(message.message);

        console.log("🔁 멤버 목록 불러오기:", currentRoomId);
        if (currentRoomId) {
            setTimeout(() => fetchRoomMembers(currentRoomId), 500);
        }
    } else {
        // 서버에서 전달받은 userId를 그대로 사용 (JWT에서 직접 추출하지 않음)
        msgDiv.className = `chat other`; // 모든 메시지를 other로 표시 (서버에서 구분)

        const contentDiv = document.createElement("div");
        contentDiv.className = "message-content";

        const bubble = document.createElement("div");
        bubble.className = "message-bubble";
        bubble.innerHTML = `<strong>${escapeHtml(message.nickname)}</strong><br>${escapeHtml(message.message)}`;

        const timeDiv = document.createElement("div");
        timeDiv.className = "message-time";

        if (message.createdAt) {
            const messageTime = new Date(message.createdAt);
            timeDiv.textContent = messageTime.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: true
            });
        }

        contentDiv.appendChild(bubble);
        contentDiv.appendChild(timeDiv);
        msgDiv.appendChild(contentDiv);
    }

    chatBox.appendChild(msgDiv);
    chatBox.scrollTop = chatBox.scrollHeight;
}

// 채팅방 리스트 조회
function fetchChatRooms() {
    clubId = parseInt(document.getElementById("clubId").value);
    const token = getToken();
    console.log("✅ 토큰값:", token);

    fetch(`${API_CHAT_URL}/api/clubs/${clubId}/chat/list`, {
        method: "GET",
        headers: {
            "Authorization": `Bearer ${token}`
        }
    })
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            return res.json();
        })
        .then(data => {
            const clubName = data.data?.clubName || "클럽명 없음";
            updateClubInfo(clubName);
            const roomListDiv = document.getElementById("chatRoomList");
            if (Array.isArray(data.data?.chatRooms) && data.data.chatRooms.length > 0) {
                roomListDiv.innerHTML = data.data.chatRooms.map(room => {
                    return `
                        <button class="room-btn" onclick="connectSocketAndSubscribe(${room.id}, '${room.name}')">
                            ${room.name}
                        </button>
                    `;
                }).join('');
            } else {
                roomListDiv.innerHTML = '<div style="color:#888;text-align:center;">채팅방이 없습니다.</div>';
            }
        })
        .catch(error => {
            handleApiError(error, '채팅방 리스트 조회에 실패했습니다.');
        });
}

// 채팅방 생성 함수
function createChatRoom() {
    const roomName = document.getElementById('roomName').value.trim();
    const imageUrl = document.getElementById('imageUrl').value.trim();
    const roomType = document.getElementById('roomType').value.trim();
    const token = getToken();

    if (!roomName || !roomType) {
        showNotification('방 이름과 타입을 입력해주세요.', 'error');
        return;
    }

    fetch(`${API_CHAT_URL}/api/clubs/${clubId}/chat/create`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({
            name: roomName,
            imageUrl: imageUrl,
            type: roomType
        })
    })
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            return res.json();
        })
        .then(data => {
            showNotification('채팅방이 생성되었습니다.', 'success');
            fetchChatRooms();
        })
        .catch(error => {
            handleApiError(error, '채팅방 생성에 실패했습니다.');
        });
}

// 알림 표시 함수
function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    
    // 기존 알림 제거
    const existingNotification = document.querySelector('.notification');
    if (existingNotification) {
        existingNotification.remove();
    }
    
    document.body.appendChild(notification);
    
    // 3초 후 자동 제거
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 3000);
}

// 메시지 전송 함수 추가
function sendMessage() {
    const messageInput = document.getElementById("messageInput");
    const message = messageInput.value.trim();
    
    if (!message || !currentRoomId || !stompClient || !stompClient.connected) {
        console.log("❌ 메시지 전송 실패:", { message, currentRoomId, connected: stompClient?.connected });
        return;
    }

    const messageData = {
        roomId: currentRoomId,
        message: message,
        type: "TALK",
        createdAt: new Date().toISOString()
    };

    console.log("전송할 메시지:", messageData);
    console.log("JSON 문자열:", JSON.stringify(messageData));
    stompClient.send("/app/message", {}, JSON.stringify(messageData));
    messageInput.value = "";
}