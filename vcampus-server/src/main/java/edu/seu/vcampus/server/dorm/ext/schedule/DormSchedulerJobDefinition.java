package edu.seu.vcampus.server.dorm.ext.schedule;

/** One scheduler registration assembled by DormSchedulerJobs. */
final class DormSchedulerJobDefinition {
    final String name;
    final String schedule;
    final DormScheduler.Plan plan;
    final DormScheduler.Task task;
    DormSchedulerJobDefinition(String name, String schedule, DormScheduler.Plan plan,
                               DormScheduler.Task task) {
        this.name = name;
        this.schedule = schedule;
        this.plan = plan;
        this.task = task;
    }
}
