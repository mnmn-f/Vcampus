package edu.seu.vcampus.common.dto.identity;

import java.io.Serializable;

/** 本人可编辑资料；不包含 userId，操作者来自会话。 */
public final class ProfileUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String displayName;
    private final String email;
    private final String phone;
    private final String avatarUrl;

    public ProfileUpdateRequest(String displayName, String email, String phone,
                                String avatarUrl) {
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
    }

    public ProfileUpdateRequest(String displayName, String email, String phone) {
        this(displayName, email, phone, null);
    }

    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
}
