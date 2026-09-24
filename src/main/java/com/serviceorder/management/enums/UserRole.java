package com.serviceorder.management.enums;

public enum UserRole {
    ADMIN,
    USER,
    // Read-only access, meant for the public demo account: it can list and download, never change anything
    VIEWER
}
