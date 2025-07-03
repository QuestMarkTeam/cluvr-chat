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
        alert('인증이 만료되었습니다. 다시 로그인해주세요.');
        clearToken();
        setTimeout(() => {
            window.location.href = '/login.html';
        }, 2000);
    } else if (error.response?.status === 403) {
        alert('접근 권한이 없습니다.');
    } else if (error.response?.status === 404) {
        alert('요청한 리소스를 찾을 수 없습니다.');
    } else if (error.response?.status >= 500) {
        alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } else {
        alert(defaultMessage);
    }
}

window.onload = async function() {
    const urlParams = new URLSearchParams(window.location.search);
    const clubId = urlParams.get('clubId');
    const token = urlParams.get('token');
    
    // 토큰을 sessionStorage에 저장
    if (token) {
        saveToken(token);
    }
    
    // 로딩 메시지 표시, 나머지 숨김
    document.getElementById('loading').style.display = 'block';
    document.getElementById('clubNameH2').style.display = 'none';
    document.getElementById('chatRoomListTitle').style.display = 'none';
    document.getElementById('chatRoomList').style.display = 'none';
    document.getElementById('createRoomSection').style.display = 'none';
    
    if (!clubId || !token) {
        alert('잘못된 접근입니다.');
        return;
    }
    
    // 채팅방 목록 및 유저 role 정보 조회
    try {
        const response = await fetch(`http://localhost:8082/api/clubs/${clubId}/chat/list`, {
            headers: { 
                'Authorization': `Bearer ${token}` 
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        
        // 클럽명 표시 (h2)
        const clubName = data.data?.clubName || "클럽명 없음";
        const clubNameH2 = document.getElementById("clubNameH2");
        if (clubNameH2) {
            clubNameH2.textContent = clubName;
            clubNameH2.style.display = 'block';
        }
        
        // 채팅방 리스트 고정 텍스트
        const chatRoomListTitle = document.getElementById("chatRoomListTitle");
        if (chatRoomListTitle) chatRoomListTitle.style.display = 'block';
        
        // 채팅방 목록 표시
        const roomListDiv = document.getElementById("chatRoomList");
        const chatRooms = data.data?.chatRooms || [];
        if (Array.isArray(chatRooms) && chatRooms.length > 0) {
            roomListDiv.innerHTML = chatRooms.map(room =>
                `<div class="chat-room-item" style="cursor:pointer; padding:10px; border-bottom:1px solid #eee;" onclick="goToChat('${room.id}', '${encodeURIComponent(clubName)}')">${room.name}</div>`
            ).join('');
        } else {
            roomListDiv.innerHTML = '<div style="text-align:center; color:#888;">채팅방이 없습니다.</div>';
        }
        roomListDiv.style.display = 'block';
        
        // 채팅방 생성 버튼 노출 조건: type이 MANAGER인 방이 하나라도 있으면 노출
        const hasManagerRoom = chatRooms.some(room => room.type === 'MANAGER');
        const createRoomSection = document.getElementById("createRoomSection");
        if (hasManagerRoom) {
            createRoomSection.style.display = "block";
        } else {
            createRoomSection.style.display = "none";
        }
        
        // 로딩 메시지 숨김
        document.getElementById('loading').style.display = 'none';
    } catch (error) {
        console.error('채팅방 목록 조회 실패:', error);
        handleApiError(error, '채팅방 목록을 불러올 수 없습니다.');
        document.getElementById("chatRoomList").innerHTML = '<div style="color:red;">채팅방 목록을 불러오지 못했습니다.</div>';
        document.getElementById('chatRoomList').style.display = 'block';
        document.getElementById('loading').style.display = 'none';
    }
};

function goToChat(roomId, clubName) {
    const urlParams = new URLSearchParams(window.location.search);
    const clubId = urlParams.get('clubId');
    const token = getToken() || urlParams.get('token');
    
    // roomName을 chatRooms에서 찾아서 같이 넘김
    const roomDiv = document.querySelector(`.chat-room-item[onclick*="${roomId}"]`);
    let roomName = '';
    if (roomDiv) {
        roomName = roomDiv.textContent.trim();
    }
    window.location.href = `chat.html?clubId=${clubId}&roomId=${roomId}&token=${encodeURIComponent(token)}&clubName=${clubName}&roomName=${encodeURIComponent(roomName)}`;
}

function goToCreateRoom() {
    const urlParams = new URLSearchParams(window.location.search);
    const clubId = urlParams.get('clubId');
    const token = getToken() || urlParams.get('token');
    window.location.href = `create-room.html?clubId=${clubId}&token=${encodeURIComponent(token)}`;
} 