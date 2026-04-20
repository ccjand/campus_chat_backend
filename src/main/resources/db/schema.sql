/*
 Navicat Premium Dump SQL

 Source Server         : 远程pgsql
 Source Server Type    : PostgreSQL
 Source Server Version : 150004 (150004)
 Source Host           : 115.190.249.67:15432
 Source Catalog        : campus_chat
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 150004 (150004)
 File Encoding         : 65001

 Date: 20/04/2026 10:00:13
*/


-- ----------------------------
-- Sequence structure for chat_contact_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_contact_id_seq";
CREATE SEQUENCE "public"."chat_contact_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."chat_contact_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for chat_group_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_group_id_seq";
CREATE SEQUENCE "public"."chat_group_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."chat_group_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for chat_group_member_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_group_member_id_seq";
CREATE SEQUENCE "public"."chat_group_member_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."chat_group_member_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for chat_message_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_message_id_seq";
CREATE SEQUENCE "public"."chat_message_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."chat_message_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for chat_message_read_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_message_read_id_seq";
CREATE SEQUENCE "public"."chat_message_read_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."chat_message_read_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for chat_room_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_room_id_seq";
CREATE SEQUENCE "public"."chat_room_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."chat_room_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for checkin_record_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."checkin_record_id_seq";
CREATE SEQUENCE "public"."checkin_record_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."checkin_record_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for checkin_session_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."checkin_session_id_seq";
CREATE SEQUENCE "public"."checkin_session_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."checkin_session_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for checkin_supplement_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."checkin_supplement_id_seq";
CREATE SEQUENCE "public"."checkin_supplement_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."checkin_supplement_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for course_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."course_id_seq";
CREATE SEQUENCE "public"."course_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."course_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for exam_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."exam_id_seq";
CREATE SEQUENCE "public"."exam_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."exam_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for leave_application_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."leave_application_id_seq";
CREATE SEQUENCE "public"."leave_application_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."leave_application_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for leave_log_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."leave_log_id_seq";
CREATE SEQUENCE "public"."leave_log_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."leave_log_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for mq_outbox_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."mq_outbox_id_seq";
CREATE SEQUENCE "public"."mq_outbox_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."mq_outbox_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for notice_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."notice_id_seq";
CREATE SEQUENCE "public"."notice_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."notice_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for sys_class_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_class_id_seq";
CREATE SEQUENCE "public"."sys_class_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."sys_class_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for sys_department_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_department_id_seq";
CREATE SEQUENCE "public"."sys_department_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."sys_department_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for sys_user_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_user_id_seq";
CREATE SEQUENCE "public"."sys_user_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."sys_user_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for user_blacklist_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_blacklist_id_seq";
CREATE SEQUENCE "public"."user_blacklist_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."user_blacklist_id_seq" OWNER TO "root";

-- ----------------------------
-- Sequence structure for user_friend_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_friend_id_seq";
CREATE SEQUENCE "public"."user_friend_id_seq"
    INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."user_friend_id_seq" OWNER TO "root";

-- ----------------------------
-- Table structure for chat_contact
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_contact";
CREATE TABLE "public"."chat_contact" (
                                         "id" int8 NOT NULL DEFAULT nextval('chat_contact_id_seq'::regclass),
                                         "user_id" int8 NOT NULL,
                                         "room_id" int8 NOT NULL,
                                         "last_msg_id" int8,
                                         "last_read_id" int8,
                                         "top" bool NOT NULL DEFAULT false,
                                         "mute" bool NOT NULL DEFAULT false,
                                         "active_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."chat_contact" OWNER TO "root";

-- ----------------------------
-- Table structure for chat_group
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_group";
CREATE TABLE "public"."chat_group" (
                                       "id" int8 NOT NULL DEFAULT nextval('chat_group_id_seq'::regclass),
                                       "room_id" int8 NOT NULL,
                                       "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                       "avatar" varchar(255) COLLATE "pg_catalog"."default",
                                       "owner_id" int8 NOT NULL,
                                       "announcement" text COLLATE "pg_catalog"."default",
                                       "type" int2 NOT NULL,
                                       "class_id" int8,
                                       "deleted" bool NOT NULL DEFAULT false,
                                       "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."chat_group" OWNER TO "root";

-- ----------------------------
-- Table structure for chat_group_member
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_group_member";
CREATE TABLE "public"."chat_group_member" (
                                              "id" int8 NOT NULL DEFAULT nextval('chat_group_member_id_seq'::regclass),
                                              "group_id" int8 NOT NULL,
                                              "user_id" int8 NOT NULL,
                                              "role" int2 NOT NULL DEFAULT 3,
                                              "nickname" varchar(32) COLLATE "pg_catalog"."default",
                                              "join_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."chat_group_member" OWNER TO "root";

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_message";
CREATE TABLE "public"."chat_message" (
                                         "id" int8 NOT NULL DEFAULT nextval('chat_message_id_seq'::regclass),
                                         "room_id" int8 NOT NULL,
                                         "from_uid" int8 NOT NULL,
                                         "type" int2 NOT NULL,
                                         "content" text COLLATE "pg_catalog"."default",
                                         "ext_info" jsonb NOT NULL DEFAULT '{}'::jsonb,
                                         "client_seq" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                         "status" int2 NOT NULL DEFAULT 0,
                                         "create_time" timestamp(6) NOT NULL DEFAULT now(),
                                         "is_visible" bool GENERATED ALWAYS AS (
                                             (status = 0)
                                             ) STORED
)
;
ALTER TABLE "public"."chat_message" OWNER TO "root";

-- ----------------------------
-- Table structure for chat_message_read
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_message_read";
CREATE TABLE "public"."chat_message_read" (
                                              "id" int8 NOT NULL DEFAULT nextval('chat_message_read_id_seq'::regclass),
                                              "msg_id" int8 NOT NULL,
                                              "room_id" int8 NOT NULL,
                                              "reader_id" int8 NOT NULL,
                                              "read_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."chat_message_read" OWNER TO "root";

-- ----------------------------
-- Table structure for chat_room
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_room";
CREATE TABLE "public"."chat_room" (
                                      "id" int8 NOT NULL DEFAULT nextval('chat_room_id_seq'::regclass),
                                      "type" int2 NOT NULL,
                                      "name" varchar(64) COLLATE "pg_catalog"."default",
                                      "ext_info" jsonb NOT NULL DEFAULT '{}'::jsonb,
                                      "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."chat_room" OWNER TO "root";

-- ----------------------------
-- Table structure for checkin_record
-- ----------------------------
DROP TABLE IF EXISTS "public"."checkin_record";
CREATE TABLE "public"."checkin_record" (
                                           "id" int8 NOT NULL DEFAULT nextval('checkin_record_id_seq'::regclass),
                                           "session_id" int8 NOT NULL,
                                           "student_id" int8 NOT NULL,
                                           "latitude" numeric(10,7),
                                           "longitude" numeric(10,7),
                                           "distance_m" int4,
                                           "status" int2 NOT NULL,
                                           "checkin_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."checkin_record" OWNER TO "root";

-- ----------------------------
-- Table structure for checkin_session
-- ----------------------------
DROP TABLE IF EXISTS "public"."checkin_session";
CREATE TABLE "public"."checkin_session" (
                                            "id" int8 NOT NULL DEFAULT nextval('checkin_session_id_seq'::regclass),
                                            "course_id" int8 NOT NULL,
                                            "creator_id" int8 NOT NULL,
                                            "title" varchar(64) COLLATE "pg_catalog"."default",
                                            "center_latitude" numeric(10,7) NOT NULL,
                                            "center_longitude" numeric(10,7) NOT NULL,
                                            "radius_meters" int4 NOT NULL,
                                            "duration_minutes" int4 NOT NULL,
                                            "start_time" timestamp(6) NOT NULL DEFAULT now(),
                                            "end_time" timestamp(6) GENERATED ALWAYS AS (
                                                (start_time + ((duration_minutes)::double precision * '00:01:00'::interval))
) STORED,
  "code" varchar(8) COLLATE "pg_catalog"."default",
  "status" int2 NOT NULL DEFAULT 1,
  "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."checkin_session" OWNER TO "root";

-- ----------------------------
-- Table structure for checkin_session_class
-- ----------------------------
DROP TABLE IF EXISTS "public"."checkin_session_class";
CREATE TABLE "public"."checkin_session_class" (
                                                  "session_id" int8 NOT NULL,
                                                  "class_id" int8 NOT NULL
)
;
ALTER TABLE "public"."checkin_session_class" OWNER TO "root";

-- ----------------------------
-- Table structure for checkin_supplement
-- ----------------------------
DROP TABLE IF EXISTS "public"."checkin_supplement";
CREATE TABLE "public"."checkin_supplement" (
                                               "id" int8 NOT NULL DEFAULT nextval('checkin_supplement_id_seq'::regclass),
                                               "session_id" int8 NOT NULL,
                                               "student_id" int8 NOT NULL,
                                               "reason" text COLLATE "pg_catalog"."default" NOT NULL,
                                               "attachment" jsonb,
                                               "status" int2 NOT NULL DEFAULT 0,
                                               "approver_id" int8,
                                               "approve_time" timestamp(6),
                                               "approve_note" varchar(255) COLLATE "pg_catalog"."default",
                                               "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."checkin_supplement" OWNER TO "root";

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS "public"."course";
CREATE TABLE "public"."course" (
                                   "id" int8 NOT NULL DEFAULT nextval('course_id_seq'::regclass),
                                   "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                   "teacher_id" int8 NOT NULL,
                                   "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."course" OWNER TO "root";

-- ----------------------------
-- Table structure for course_class_rel
-- ----------------------------
DROP TABLE IF EXISTS "public"."course_class_rel";
CREATE TABLE "public"."course_class_rel" (
                                             "course_id" int8 NOT NULL,
                                             "class_id" int8 NOT NULL
)
;
ALTER TABLE "public"."course_class_rel" OWNER TO "root";

-- ----------------------------
-- Table structure for exam
-- ----------------------------
DROP TABLE IF EXISTS "public"."exam";
CREATE TABLE "public"."exam" (
                                 "id" int8 NOT NULL DEFAULT nextval('exam_id_seq'::regclass),
                                 "course_id" int8 NOT NULL,
                                 "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                 "exam_time" timestamp(6) NOT NULL,
                                 "duration_minutes" int4 NOT NULL,
                                 "location" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                 "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."exam" OWNER TO "root";

-- ----------------------------
-- Table structure for exam_student_rel
-- ----------------------------
DROP TABLE IF EXISTS "public"."exam_student_rel";
CREATE TABLE "public"."exam_student_rel" (
                                             "exam_id" int8 NOT NULL,
                                             "student_id" int8 NOT NULL,
                                             "seat_no" varchar(32) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."exam_student_rel" OWNER TO "root";

-- ----------------------------
-- Table structure for leave_application
-- ----------------------------
DROP TABLE IF EXISTS "public"."leave_application";
CREATE TABLE "public"."leave_application" (
                                              "id" int8 NOT NULL DEFAULT nextval('leave_application_id_seq'::regclass),
                                              "applicant_id" int8 NOT NULL,
                                              "approver_id" int8 NOT NULL,
                                              "type" int2 NOT NULL,
                                              "reason" text COLLATE "pg_catalog"."default" NOT NULL,
                                              "start_time" timestamp(6) NOT NULL,
                                              "end_time" timestamp(6) NOT NULL,
                                              "attachments" jsonb NOT NULL DEFAULT '[]'::jsonb,
                                              "status" int2 NOT NULL DEFAULT 0,
                                              "approve_note" varchar(255) COLLATE "pg_catalog"."default",
                                              "approve_time" timestamp(6),
                                              "card_msg_id" int8,
                                              "create_time" timestamp(6) NOT NULL DEFAULT now(),
                                              "update_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."leave_application" OWNER TO "root";

-- ----------------------------
-- Table structure for leave_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."leave_log";
CREATE TABLE "public"."leave_log" (
                                      "id" int8 NOT NULL DEFAULT nextval('leave_log_id_seq'::regclass),
                                      "leave_id" int8 NOT NULL,
                                      "from_status" int2,
                                      "to_status" int2 NOT NULL,
                                      "operator_id" int8 NOT NULL,
                                      "remark" varchar(255) COLLATE "pg_catalog"."default",
                                      "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."leave_log" OWNER TO "root";

-- ----------------------------
-- Table structure for mq_outbox
-- ----------------------------
DROP TABLE IF EXISTS "public"."mq_outbox";
CREATE TABLE "public"."mq_outbox" (
                                      "id" int8 NOT NULL DEFAULT nextval('mq_outbox_id_seq'::regclass),
                                      "topic" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                      "tag" varchar(32) COLLATE "pg_catalog"."default",
                                      "payload" jsonb NOT NULL,
                                      "status" int2 NOT NULL DEFAULT 0,
                                      "retry_count" int2 NOT NULL DEFAULT 0,
                                      "next_retry_time" timestamp(6),
                                      "create_time" timestamp(6) NOT NULL DEFAULT now(),
                                      "send_time" timestamp(6)
)
;
ALTER TABLE "public"."mq_outbox" OWNER TO "root";

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS "public"."notice";
CREATE TABLE "public"."notice" (
                                   "id" int8 NOT NULL DEFAULT nextval('notice_id_seq'::regclass),
                                   "title" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                   "content" text COLLATE "pg_catalog"."default" NOT NULL,
                                   "publisher_id" int8 NOT NULL,
                                   "scope_type" int2 NOT NULL,
                                   "scope_data" jsonb NOT NULL DEFAULT '{}'::jsonb,
                                   "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."notice" OWNER TO "root";

-- ----------------------------
-- Table structure for notice_read
-- ----------------------------
DROP TABLE IF EXISTS "public"."notice_read";
CREATE TABLE "public"."notice_read" (
                                        "notice_id" int8 NOT NULL,
                                        "user_id" int8 NOT NULL,
                                        "read_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."notice_read" OWNER TO "root";

-- ----------------------------
-- Table structure for sys_class
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_class";
CREATE TABLE "public"."sys_class" (
                                      "id" int8 NOT NULL DEFAULT nextval('sys_class_id_seq'::regclass),
                                      "department_id" int8 NOT NULL,
                                      "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                      "grade" int2 NOT NULL,
                                      "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."sys_class" OWNER TO "root";

-- ----------------------------
-- Table structure for sys_department
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_department";
CREATE TABLE "public"."sys_department" (
                                           "id" int8 NOT NULL DEFAULT nextval('sys_department_id_seq'::regclass),
                                           "name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
                                           "code" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
                                           "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."sys_department" OWNER TO "root";

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user";
CREATE TABLE "public"."sys_user" (
                                     "id" int8 NOT NULL DEFAULT nextval('sys_user_id_seq'::regclass),
                                     "account_number" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
                                     "password_hash" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
                                     "name" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
                                     "role" int2 NOT NULL,
                                     "department_id" int8,
                                     "avatar" varchar(255) COLLATE "pg_catalog"."default",
                                     "phone" varchar(32) COLLATE "pg_catalog"."default",
                                     "email" varchar(128) COLLATE "pg_catalog"."default",
                                     "enabled" bool NOT NULL DEFAULT true,
                                     "create_time" timestamp(6) NOT NULL DEFAULT now(),
                                     "update_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."sys_user" OWNER TO "root";

-- ----------------------------
-- Table structure for sys_user_class_rel
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_class_rel";
CREATE TABLE "public"."sys_user_class_rel" (
                                               "user_id" int8 NOT NULL,
                                               "class_id" int8 NOT NULL
)
;
ALTER TABLE "public"."sys_user_class_rel" OWNER TO "root";

-- ----------------------------
-- Table structure for user_blacklist
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_blacklist";
CREATE TABLE "public"."user_blacklist" (
                                           "id" int8 NOT NULL DEFAULT nextval('user_blacklist_id_seq'::regclass),
                                           "user_id" int8 NOT NULL,
                                           "target_id" int8 NOT NULL,
                                           "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."user_blacklist" OWNER TO "root";

-- ----------------------------
-- Table structure for user_friend
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_friend";
CREATE TABLE "public"."user_friend" (
                                        "id" int8 NOT NULL DEFAULT nextval('user_friend_id_seq'::regclass),
                                        "user_id" int8 NOT NULL,
                                        "friend_id" int8 NOT NULL,
                                        "remark" varchar(32) COLLATE "pg_catalog"."default",
                                        "deleted" bool NOT NULL DEFAULT false,
                                        "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;
ALTER TABLE "public"."user_friend" OWNER TO "root";

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_contact_id_seq"
    OWNED BY "public"."chat_contact"."id";
SELECT setval('"public"."chat_contact_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_group_id_seq"
    OWNED BY "public"."chat_group"."id";
SELECT setval('"public"."chat_group_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_group_member_id_seq"
    OWNED BY "public"."chat_group_member"."id";
SELECT setval('"public"."chat_group_member_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_message_id_seq"
    OWNED BY "public"."chat_message"."id";
SELECT setval('"public"."chat_message_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_message_read_id_seq"
    OWNED BY "public"."chat_message_read"."id";
SELECT setval('"public"."chat_message_read_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_room_id_seq"
    OWNED BY "public"."chat_room"."id";
SELECT setval('"public"."chat_room_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."checkin_record_id_seq"
    OWNED BY "public"."checkin_record"."id";
SELECT setval('"public"."checkin_record_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."checkin_session_id_seq"
    OWNED BY "public"."checkin_session"."id";
SELECT setval('"public"."checkin_session_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."checkin_supplement_id_seq"
    OWNED BY "public"."checkin_supplement"."id";
SELECT setval('"public"."checkin_supplement_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."course_id_seq"
    OWNED BY "public"."course"."id";
SELECT setval('"public"."course_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."exam_id_seq"
    OWNED BY "public"."exam"."id";
SELECT setval('"public"."exam_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."leave_application_id_seq"
    OWNED BY "public"."leave_application"."id";
SELECT setval('"public"."leave_application_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."leave_log_id_seq"
    OWNED BY "public"."leave_log"."id";
SELECT setval('"public"."leave_log_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."mq_outbox_id_seq"
    OWNED BY "public"."mq_outbox"."id";
SELECT setval('"public"."mq_outbox_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."notice_id_seq"
    OWNED BY "public"."notice"."id";
SELECT setval('"public"."notice_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_class_id_seq"
    OWNED BY "public"."sys_class"."id";
SELECT setval('"public"."sys_class_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_department_id_seq"
    OWNED BY "public"."sys_department"."id";
SELECT setval('"public"."sys_department_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_user_id_seq"
    OWNED BY "public"."sys_user"."id";
SELECT setval('"public"."sys_user_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_blacklist_id_seq"
    OWNED BY "public"."user_blacklist"."id";
SELECT setval('"public"."user_blacklist_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_friend_id_seq"
    OWNED BY "public"."user_friend"."id";
SELECT setval('"public"."user_friend_id_seq"', 1, false);

-- ----------------------------
-- Indexes structure for table chat_contact
-- ----------------------------
CREATE INDEX "idx_contact_user_active" ON "public"."chat_contact" USING btree (
    "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
    "active_time" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
    );

-- ----------------------------
-- Uniques structure for table chat_contact
-- ----------------------------
ALTER TABLE "public"."chat_contact" ADD CONSTRAINT "uk_contact" UNIQUE ("user_id", "room_id");

-- ----------------------------
-- Primary Key structure for table chat_contact
-- ----------------------------
ALTER TABLE "public"."chat_contact" ADD CONSTRAINT "chat_contact_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table chat_group
-- ----------------------------
ALTER TABLE "public"."chat_group" ADD CONSTRAINT "chat_group_room_id_key" UNIQUE ("room_id");

-- ----------------------------
-- Primary Key structure for table chat_group
-- ----------------------------
ALTER TABLE "public"."chat_group" ADD CONSTRAINT "chat_group_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table chat_group_member
-- ----------------------------
ALTER TABLE "public"."chat_group_member" ADD CONSTRAINT "uk_group_member" UNIQUE ("group_id", "user_id");

-- ----------------------------
-- Primary Key structure for table chat_group_member
-- ----------------------------
ALTER TABLE "public"."chat_group_member" ADD CONSTRAINT "chat_group_member_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table chat_message
-- ----------------------------
CREATE INDEX "idx_msg_room_time_visible" ON "public"."chat_message" USING btree (
    "room_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
    "create_time" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
    ) WHERE is_visible = true;

-- ----------------------------
-- Uniques structure for table chat_message
-- ----------------------------
ALTER TABLE "public"."chat_message" ADD CONSTRAINT "uk_msg_idem" UNIQUE ("from_uid", "room_id", "client_seq");

-- ----------------------------
-- Primary Key structure for table chat_message
-- ----------------------------
ALTER TABLE "public"."chat_message" ADD CONSTRAINT "chat_message_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table chat_message_read
-- ----------------------------
ALTER TABLE "public"."chat_message_read" ADD CONSTRAINT "uk_msg_read" UNIQUE ("msg_id", "reader_id");

-- ----------------------------
-- Primary Key structure for table chat_message_read
-- ----------------------------
ALTER TABLE "public"."chat_message_read" ADD CONSTRAINT "chat_message_read_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table chat_room
-- ----------------------------
ALTER TABLE "public"."chat_room" ADD CONSTRAINT "chat_room_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table checkin_record
-- ----------------------------
ALTER TABLE "public"."checkin_record" ADD CONSTRAINT "uk_checkin_record" UNIQUE ("session_id", "student_id");

-- ----------------------------
-- Primary Key structure for table checkin_record
-- ----------------------------
ALTER TABLE "public"."checkin_record" ADD CONSTRAINT "checkin_record_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table checkin_session
-- ----------------------------
CREATE INDEX "idx_checkin_session_course_active" ON "public"."checkin_session" USING btree (
    "course_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
    "start_time" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
    ) WHERE status = 1;

-- ----------------------------
-- Primary Key structure for table checkin_session
-- ----------------------------
ALTER TABLE "public"."checkin_session" ADD CONSTRAINT "checkin_session_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table checkin_session_class
-- ----------------------------
ALTER TABLE "public"."checkin_session_class" ADD CONSTRAINT "checkin_session_class_pkey" PRIMARY KEY ("session_id", "class_id");

-- ----------------------------
-- Primary Key structure for table checkin_supplement
-- ----------------------------
ALTER TABLE "public"."checkin_supplement" ADD CONSTRAINT "checkin_supplement_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table course
-- ----------------------------
ALTER TABLE "public"."course" ADD CONSTRAINT "course_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table course_class_rel
-- ----------------------------
ALTER TABLE "public"."course_class_rel" ADD CONSTRAINT "course_class_rel_pkey" PRIMARY KEY ("course_id", "class_id");

-- ----------------------------
-- Primary Key structure for table exam
-- ----------------------------
ALTER TABLE "public"."exam" ADD CONSTRAINT "exam_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table exam_student_rel
-- ----------------------------
CREATE INDEX "idx_exam_student_time" ON "public"."exam_student_rel" USING btree (
    "student_id" "pg_catalog"."int8_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table exam_student_rel
-- ----------------------------
ALTER TABLE "public"."exam_student_rel" ADD CONSTRAINT "exam_student_rel_pkey" PRIMARY KEY ("exam_id", "student_id");

-- ----------------------------
-- Indexes structure for table leave_application
-- ----------------------------
CREATE INDEX "idx_leave_approver_pending" ON "public"."leave_application" USING btree (
    "approver_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
    "create_time" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
    ) WHERE status = 0;

-- ----------------------------
-- Primary Key structure for table leave_application
-- ----------------------------
ALTER TABLE "public"."leave_application" ADD CONSTRAINT "leave_application_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table leave_log
-- ----------------------------
ALTER TABLE "public"."leave_log" ADD CONSTRAINT "leave_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table mq_outbox
-- ----------------------------
CREATE INDEX "idx_outbox_status_retry" ON "public"."mq_outbox" USING btree (
    "status" "pg_catalog"."int2_ops" ASC NULLS LAST,
    "next_retry_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
    ) WHERE status = ANY (ARRAY[0, 2]);

-- ----------------------------
-- Primary Key structure for table mq_outbox
-- ----------------------------
ALTER TABLE "public"."mq_outbox" ADD CONSTRAINT "mq_outbox_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table notice
-- ----------------------------
ALTER TABLE "public"."notice" ADD CONSTRAINT "notice_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table notice_read
-- ----------------------------
ALTER TABLE "public"."notice_read" ADD CONSTRAINT "notice_read_pkey" PRIMARY KEY ("notice_id", "user_id");

-- ----------------------------
-- Primary Key structure for table sys_class
-- ----------------------------
ALTER TABLE "public"."sys_class" ADD CONSTRAINT "sys_class_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_department
-- ----------------------------
ALTER TABLE "public"."sys_department" ADD CONSTRAINT "sys_department_name_key" UNIQUE ("name");
ALTER TABLE "public"."sys_department" ADD CONSTRAINT "sys_department_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table sys_department
-- ----------------------------
ALTER TABLE "public"."sys_department" ADD CONSTRAINT "sys_department_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_user
-- ----------------------------
CREATE INDEX "idx_user_enabled_role" ON "public"."sys_user" USING btree (
    "role" "pg_catalog"."int2_ops" ASC NULLS LAST
    ) WHERE enabled = true;

-- ----------------------------
-- Uniques structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_account_number_key" UNIQUE ("account_number");

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user_class_rel
-- ----------------------------
ALTER TABLE "public"."sys_user_class_rel" ADD CONSTRAINT "sys_user_class_rel_pkey" PRIMARY KEY ("user_id", "class_id");

-- ----------------------------
-- Uniques structure for table user_blacklist
-- ----------------------------
ALTER TABLE "public"."user_blacklist" ADD CONSTRAINT "uk_user_blacklist" UNIQUE ("user_id", "target_id");

-- ----------------------------
-- Primary Key structure for table user_blacklist
-- ----------------------------
ALTER TABLE "public"."user_blacklist" ADD CONSTRAINT "user_blacklist_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table user_friend
-- ----------------------------
ALTER TABLE "public"."user_friend" ADD CONSTRAINT "uk_user_friend" UNIQUE ("user_id", "friend_id");

-- ----------------------------
-- Primary Key structure for table user_friend
-- ----------------------------
ALTER TABLE "public"."user_friend" ADD CONSTRAINT "user_friend_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table sys_class
-- ----------------------------
ALTER TABLE "public"."sys_class" ADD CONSTRAINT "sys_class_department_id_fkey" FOREIGN KEY ("department_id") REFERENCES "public"."sys_department" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_user_class_rel
-- ----------------------------
ALTER TABLE "public"."sys_user_class_rel" ADD CONSTRAINT "sys_user_class_rel_class_id_fkey" FOREIGN KEY ("class_id") REFERENCES "public"."sys_class" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."sys_user_class_rel" ADD CONSTRAINT "sys_user_class_rel_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."sys_user" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;


-- 好友申请表
CREATE TABLE IF NOT EXISTS friend_request (
                                              id          BIGSERIAL PRIMARY KEY,
                                              from_id     BIGINT NOT NULL REFERENCES sys_user(id),   -- 申请人
    to_id       BIGINT NOT NULL REFERENCES sys_user(id),   -- 被申请人
    reason      VARCHAR(128),                               -- 申请理由
    status      SMALLINT NOT NULL DEFAULT 0,                -- 0=待处理 1=已同意 2=已拒绝
    create_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(from_id, to_id)                                  -- 防止重复申请
    );
CREATE INDEX IF NOT EXISTS idx_friend_request_to_status
    ON friend_request(to_id, status);