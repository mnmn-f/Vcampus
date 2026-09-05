-- ============================================================================
-- V8 住宿申请不再要求学生自己填目标床位
--
-- 原来的 ck_accommodation_requests_bed 强制入住和调宿申请必须带 requested_bed_id。
-- 这条约束把「哪张床空着」这个只有宿管才知道的信息，压给了提交申请的学生：他要么
-- 去猜一个编号，要么填错把申请挂到别人的床上。改成宿管在审批那一步指定床位之后，
-- 申请单在待审批期间 requested_bed_id 就是空的，约束必须放开。
--
-- 退宿仍然不允许带床位——那一列对退宿没有任何含义。
-- ============================================================================

ALTER TABLE `accommodation_requests` DROP CHECK `ck_accommodation_requests_bed`;
ALTER TABLE `accommodation_requests` ADD CONSTRAINT `ck_accommodation_requests_bed`
    CHECK (`request_type` <> 'CHECK_OUT' OR `requested_bed_id` IS NULL);
