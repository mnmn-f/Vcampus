-- ============================================================================
-- V7 报修工单增加「待宿管审核」状态，并修正演示楼栋的性别政策
--
-- 原来的四态里，维修员一点「完工」工单就直接终结，宿管没有任何复核的机会——而现实
-- 里报修完没完，是宿管去看一眼才算数。这里在 IN_PROGRESS 和 COMPLETED 之间插一个
-- PENDING_REVIEW：维修员报完工进入待审核，宿管审核通过才算 COMPLETED，打回则退回
-- IN_PROGRESS 由同一个维修员继续。
--
-- completed_at 仍然只在真正 COMPLETED 时写入，所以 ck_repair_orders_completed_at
-- 不用动。
-- ============================================================================

ALTER TABLE `repair_orders` DROP CHECK `ck_repair_orders_status`;
ALTER TABLE `repair_orders` ADD CONSTRAINT `ck_repair_orders_status`
    CHECK (`status` IN ('SUBMITTED', 'ACCEPTED', 'IN_PROGRESS', 'PENDING_REVIEW',
                        'COMPLETED', 'CANCELLED'));

-- 本校没有混住楼栋。演示数据里那栋建成了 MIXED，改成女生楼；
-- 界面上已经不再提供混住选项，这里把存量数据一并对齐。
UPDATE `dorm_buildings` SET `gender_policy` = 'FEMALE' WHERE `gender_policy` = 'MIXED';
