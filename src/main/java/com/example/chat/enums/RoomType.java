package com.example.chat.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum RoomType {
	MANAGER, MEMBER
}
