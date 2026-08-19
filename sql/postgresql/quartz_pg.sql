-- ----------------------------
-- Quartz PostgreSQL 版建表脚本
-- 由 sql/quartz.sql（MySQL）转换而来，MySQL 原脚本保留不动
--
-- 说明：RuoYi 默认 ScheduleConfig 整个类是注释状态，Quartz 走内存 RAMJobStore，
--       这些表不参与启动。仅当打开 ScheduleConfig（集群/持久化调度）时才需要导入本脚本，
--       并在 ScheduleConfig 的 Properties 中补一行：
--       prop.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
--
-- 转换要点：
--   blob        -> bytea
--   varchar(1)  -> bool（Quartz StdJDBCDelegate 对这些列用 setBoolean，PG 下必须是 bool）
--   bigint(13)  -> int8，smallint(2) -> int2，integer 保持
--   内联 comment -> comment on column
-- ----------------------------

drop table if exists qrtz_fired_triggers;
drop table if exists qrtz_paused_trigger_grps;
drop table if exists qrtz_scheduler_state;
drop table if exists qrtz_locks;
drop table if exists qrtz_simple_triggers;
drop table if exists qrtz_simprop_triggers;
drop table if exists qrtz_cron_triggers;
drop table if exists qrtz_blob_triggers;
drop table if exists qrtz_triggers;
drop table if exists qrtz_job_details;
drop table if exists qrtz_calendars;

-- ----------------------------
-- 1、存储每一个已配置的 jobDetail 的详细信息
-- ----------------------------
create table qrtz_job_details (
    sched_name           varchar(120)    not null,
    job_name             varchar(200)    not null,
    job_group            varchar(200)    not null,
    description          varchar(250)    null,
    job_class_name       varchar(250)    not null,
    is_durable           bool            not null,
    is_nonconcurrent     bool            not null,
    is_update_data       bool            not null,
    requests_recovery    bool            not null,
    job_data             bytea           null,
    primary key (sched_name, job_name, job_group)
);

comment on table qrtz_job_details is '任务详细信息表';
comment on column qrtz_job_details.sched_name is '调度名称';
comment on column qrtz_job_details.job_name is '任务名称';
comment on column qrtz_job_details.job_group is '任务组名';
comment on column qrtz_job_details.description is '相关介绍';
comment on column qrtz_job_details.job_class_name is '执行任务类名称';
comment on column qrtz_job_details.is_durable is '是否持久化';
comment on column qrtz_job_details.is_nonconcurrent is '是否并发';
comment on column qrtz_job_details.is_update_data is '是否更新数据';
comment on column qrtz_job_details.requests_recovery is '是否接受恢复执行';
comment on column qrtz_job_details.job_data is '存放持久化job对象';

-- ----------------------------
-- 2、 存储已配置的 Trigger 的信息
-- ----------------------------
create table qrtz_triggers (
    sched_name           varchar(120)    not null,
    trigger_name         varchar(200)    not null,
    trigger_group        varchar(200)    not null,
    job_name             varchar(200)    not null,
    job_group            varchar(200)    not null,
    description          varchar(250)    null,
    next_fire_time       int8            null,
    prev_fire_time       int8            null,
    priority             integer         null,
    trigger_state        varchar(16)     not null,
    trigger_type         varchar(8)      not null,
    start_time           int8            not null,
    end_time             int8            null,
    calendar_name        varchar(200)    null,
    misfire_instr        int2            null,
    job_data             bytea           null,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, job_name, job_group) references qrtz_job_details(sched_name, job_name, job_group)
);

comment on table qrtz_triggers is '触发器详细信息表';
comment on column qrtz_triggers.sched_name is '调度名称';
comment on column qrtz_triggers.trigger_name is '触发器的名字';
comment on column qrtz_triggers.trigger_group is '触发器所属组的名字';
comment on column qrtz_triggers.job_name is 'qrtz_job_details表job_name的外键';
comment on column qrtz_triggers.job_group is 'qrtz_job_details表job_group的外键';
comment on column qrtz_triggers.description is '相关介绍';
comment on column qrtz_triggers.next_fire_time is '上一次触发时间（毫秒）';
comment on column qrtz_triggers.prev_fire_time is '下一次触发时间（默认为-1表示不触发）';
comment on column qrtz_triggers.priority is '优先级';
comment on column qrtz_triggers.trigger_state is '触发器状态';
comment on column qrtz_triggers.trigger_type is '触发器的类型';
comment on column qrtz_triggers.start_time is '开始时间';
comment on column qrtz_triggers.end_time is '结束时间';
comment on column qrtz_triggers.calendar_name is '日程表名称';
comment on column qrtz_triggers.misfire_instr is '补偿执行的策略';
comment on column qrtz_triggers.job_data is '存放持久化job对象';

-- ----------------------------
-- 3、 存储简单的 Trigger，包括重复次数，间隔，以及已触发的次数
-- ----------------------------
create table qrtz_simple_triggers (
    sched_name           varchar(120)    not null,
    trigger_name         varchar(200)    not null,
    trigger_group        varchar(200)    not null,
    repeat_count         int8            not null,
    repeat_interval      int8            not null,
    times_triggered      int8            not null,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group) references qrtz_triggers(sched_name, trigger_name, trigger_group)
);

comment on table qrtz_simple_triggers is '简单触发器的信息表';
comment on column qrtz_simple_triggers.sched_name is '调度名称';
comment on column qrtz_simple_triggers.trigger_name is 'qrtz_triggers表trigger_name的外键';
comment on column qrtz_simple_triggers.trigger_group is 'qrtz_triggers表trigger_group的外键';
comment on column qrtz_simple_triggers.repeat_count is '重复的次数统计';
comment on column qrtz_simple_triggers.repeat_interval is '重复的间隔时间';
comment on column qrtz_simple_triggers.times_triggered is '已经触发的次数';

-- ----------------------------
-- 4、 存储 Cron Trigger，包括 Cron 表达式和时区信息
-- ----------------------------
create table qrtz_cron_triggers (
    sched_name           varchar(120)    not null,
    trigger_name         varchar(200)    not null,
    trigger_group        varchar(200)    not null,
    cron_expression      varchar(200)    not null,
    time_zone_id         varchar(80),
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group) references qrtz_triggers(sched_name, trigger_name, trigger_group)
);

comment on table qrtz_cron_triggers is 'Cron类型的触发器表';
comment on column qrtz_cron_triggers.sched_name is '调度名称';
comment on column qrtz_cron_triggers.trigger_name is 'qrtz_triggers表trigger_name的外键';
comment on column qrtz_cron_triggers.trigger_group is 'qrtz_triggers表trigger_group的外键';
comment on column qrtz_cron_triggers.cron_expression is 'cron表达式';
comment on column qrtz_cron_triggers.time_zone_id is '时区';

-- ----------------------------
-- 5、 Trigger 作为 Blob 类型存储
-- ----------------------------
create table qrtz_blob_triggers (
    sched_name           varchar(120)    not null,
    trigger_name         varchar(200)    not null,
    trigger_group        varchar(200)    not null,
    blob_data            bytea           null,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group) references qrtz_triggers(sched_name, trigger_name, trigger_group)
);

comment on table qrtz_blob_triggers is 'Blob类型的触发器表';
comment on column qrtz_blob_triggers.sched_name is '调度名称';
comment on column qrtz_blob_triggers.trigger_name is 'qrtz_triggers表trigger_name的外键';
comment on column qrtz_blob_triggers.trigger_group is 'qrtz_triggers表trigger_group的外键';
comment on column qrtz_blob_triggers.blob_data is '存放持久化Trigger对象';

-- ----------------------------
-- 6、 以 Blob 类型存储存放日历信息
-- ----------------------------
create table qrtz_calendars (
    sched_name           varchar(120)    not null,
    calendar_name        varchar(200)    not null,
    calendar             bytea           not null,
    primary key (sched_name, calendar_name)
);

comment on table qrtz_calendars is '日历信息表';
comment on column qrtz_calendars.sched_name is '调度名称';
comment on column qrtz_calendars.calendar_name is '日历名称';
comment on column qrtz_calendars.calendar is '存放持久化calendar对象';

-- ----------------------------
-- 7、 存储已暂停的 Trigger 组的信息
-- ----------------------------
create table qrtz_paused_trigger_grps (
    sched_name           varchar(120)    not null,
    trigger_group        varchar(200)    not null,
    primary key (sched_name, trigger_group)
);

comment on table qrtz_paused_trigger_grps is '暂停的触发器表';
comment on column qrtz_paused_trigger_grps.sched_name is '调度名称';
comment on column qrtz_paused_trigger_grps.trigger_group is 'qrtz_triggers表trigger_group的外键';

-- ----------------------------
-- 8、 存储与已触发的 Trigger 相关的状态信息，以及相联 Job 的执行信息
-- ----------------------------
create table qrtz_fired_triggers (
    sched_name           varchar(120)    not null,
    entry_id             varchar(95)     not null,
    trigger_name         varchar(200)    not null,
    trigger_group        varchar(200)    not null,
    instance_name        varchar(200)    not null,
    fired_time           int8            not null,
    sched_time           int8            not null,
    priority             integer         not null,
    state                varchar(16)     not null,
    job_name             varchar(200)    null,
    job_group            varchar(200)    null,
    is_nonconcurrent     bool            null,
    requests_recovery    bool            null,
    primary key (sched_name, entry_id)
);

comment on table qrtz_fired_triggers is '已触发的触发器表';
comment on column qrtz_fired_triggers.sched_name is '调度名称';
comment on column qrtz_fired_triggers.entry_id is '调度器实例id';
comment on column qrtz_fired_triggers.trigger_name is 'qrtz_triggers表trigger_name的外键';
comment on column qrtz_fired_triggers.trigger_group is 'qrtz_triggers表trigger_group的外键';
comment on column qrtz_fired_triggers.instance_name is '调度器实例名';
comment on column qrtz_fired_triggers.fired_time is '触发的时间';
comment on column qrtz_fired_triggers.sched_time is '定时器制定的时间';
comment on column qrtz_fired_triggers.priority is '优先级';
comment on column qrtz_fired_triggers.state is '状态';
comment on column qrtz_fired_triggers.job_name is '任务名称';
comment on column qrtz_fired_triggers.job_group is '任务组名';
comment on column qrtz_fired_triggers.is_nonconcurrent is '是否并发';
comment on column qrtz_fired_triggers.requests_recovery is '是否接受恢复执行';

-- ----------------------------
-- 9、 存储少量的有关 Scheduler 的状态信息
-- ----------------------------
create table qrtz_scheduler_state (
    sched_name           varchar(120)    not null,
    instance_name        varchar(200)    not null,
    last_checkin_time    int8            not null,
    checkin_interval     int8            not null,
    primary key (sched_name, instance_name)
);

comment on table qrtz_scheduler_state is '调度器状态表';
comment on column qrtz_scheduler_state.sched_name is '调度名称';
comment on column qrtz_scheduler_state.instance_name is '实例名称';
comment on column qrtz_scheduler_state.last_checkin_time is '上次检查时间';
comment on column qrtz_scheduler_state.checkin_interval is '检查间隔时间';

-- ----------------------------
-- 10、 存储程序的悲观锁的信息
-- ----------------------------
create table qrtz_locks (
    sched_name           varchar(120)    not null,
    lock_name            varchar(40)     not null,
    primary key (sched_name, lock_name)
);

comment on table qrtz_locks is '存储的悲观锁信息表';
comment on column qrtz_locks.sched_name is '调度名称';
comment on column qrtz_locks.lock_name is '悲观锁名称';

-- ----------------------------
-- 11、 Quartz集群实现同步机制的行锁表
-- ----------------------------
create table qrtz_simprop_triggers (
    sched_name           varchar(120)    not null,
    trigger_name         varchar(200)    not null,
    trigger_group        varchar(200)    not null,
    str_prop_1           varchar(512)    null,
    str_prop_2           varchar(512)    null,
    str_prop_3           varchar(512)    null,
    int_prop_1           int4            null,
    int_prop_2           int4            null,
    long_prop_1          int8            null,
    long_prop_2          int8            null,
    dec_prop_1           numeric(13,4)   null,
    dec_prop_2           numeric(13,4)   null,
    bool_prop_1          bool            null,
    bool_prop_2          bool            null,
    primary key (sched_name, trigger_name, trigger_group),
    foreign key (sched_name, trigger_name, trigger_group) references qrtz_triggers(sched_name, trigger_name, trigger_group)
);

comment on table qrtz_simprop_triggers is '同步机制的行锁表';
comment on column qrtz_simprop_triggers.sched_name is '调度名称';
comment on column qrtz_simprop_triggers.trigger_name is 'qrtz_triggers表trigger_name的外键';
comment on column qrtz_simprop_triggers.trigger_group is 'qrtz_triggers表trigger_group的外键';
comment on column qrtz_simprop_triggers.str_prop_1 is 'String类型的trigger的第一个参数';
comment on column qrtz_simprop_triggers.str_prop_2 is 'String类型的trigger的第二个参数';
comment on column qrtz_simprop_triggers.str_prop_3 is 'String类型的trigger的第三个参数';
comment on column qrtz_simprop_triggers.int_prop_1 is 'int类型的trigger的第一个参数';
comment on column qrtz_simprop_triggers.int_prop_2 is 'int类型的trigger的第二个参数';
comment on column qrtz_simprop_triggers.long_prop_1 is 'long类型的trigger的第一个参数';
comment on column qrtz_simprop_triggers.long_prop_2 is 'long类型的trigger的第二个参数';
comment on column qrtz_simprop_triggers.dec_prop_1 is 'decimal类型的trigger的第一个参数';
comment on column qrtz_simprop_triggers.dec_prop_2 is 'decimal类型的trigger的第二个参数';
comment on column qrtz_simprop_triggers.bool_prop_1 is 'Boolean类型的trigger的第一个参数';
comment on column qrtz_simprop_triggers.bool_prop_2 is 'Boolean类型的trigger的第二个参数';

commit;
