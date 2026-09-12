package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 可建立学籍的学生账号；不跨端暴露数据库用户主键。 */
public final class StudentAccountCandidateDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String account;
    private final String displayName;
    private final String email;
    private final String phone;
    private final LocalDateTime registeredAt;

    public StudentAccountCandidateDto(String account, String displayName, String email,
                                      String phone, LocalDateTime registeredAt) {
        this.account = account;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.registeredAt = registeredAt;
    }

    public String getAccount() { return account; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }

    @Override public String toString() {
        String name = displayName == null || displayName.trim().isEmpty()
                ? "未填写姓名" : displayName;
        return name + "（" + account + "）";
    }
}
