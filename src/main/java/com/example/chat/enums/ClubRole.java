package com.example.chat.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ClubRole {
	LEADER, MANAGER, MEMBER
}
