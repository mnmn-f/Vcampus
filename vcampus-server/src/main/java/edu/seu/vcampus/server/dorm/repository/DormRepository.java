package edu.seu.vcampus.server.dorm.repository;

/** 便于服务和注册器注入的宿舍仓储组合；具体实现仍按职责拆分。 */
public interface DormRepository extends DormFacilityRepository,
        DormSpaceRepository, DormLeaveRepository,
        DormAccommodationRepository, DormGovernanceRepository,
        DormBillingRepository, DormAnnouncementRepository {
}
