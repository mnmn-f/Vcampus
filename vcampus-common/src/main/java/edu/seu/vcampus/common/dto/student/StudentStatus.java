package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 学籍状态。 */
public enum StudentStatus implements Serializable {
    ENROLLED,
    SUSPENDED,
    GRADUATED,
    WITHDRAWN
}
