CREATE TABLE IF NOT EXISTS app_registration (
                                                id bigint NOT NULL PRIMARY KEY,
                                                object_version bigint,
                                                default_version boolean,
                                                metadata_uri text,
                                                name character varying(255),
    type integer,
    uri text,
    version character varying(255)
    );

CREATE TABLE IF NOT EXISTS audit_records (
                                             id bigint NOT NULL PRIMARY KEY,
                                             audit_action bigint,
                                             audit_data text,
                                             audit_operation bigint,
                                             correlation_id character varying(255),
    created_by character varying(255),
    created_on timestamp without time zone,
    platform_name character varying(255)
    );

CREATE TABLE IF NOT EXISTS batch_job_instance (
                                                  job_instance_id bigint NOT NULL PRIMARY KEY,
                                                  version bigint,
                                                  job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL,
    CONSTRAINT job_inst_un UNIQUE (job_name, job_key)
    );

CREATE TABLE IF NOT EXISTS batch_job_execution (
                                                   job_execution_id bigint NOT NULL PRIMARY KEY,
                                                   version bigint,
                                                   job_instance_id bigint NOT NULL,
                                                   create_time timestamp without time zone NOT NULL,
                                                   start_time timestamp without time zone,
                                                   end_time timestamp without time zone,
                                                   status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone,
    job_configuration_location character varying(2500),
    CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES batch_job_instance(job_instance_id)
    );

CREATE TABLE IF NOT EXISTS batch_job_execution_params (
                                                          job_execution_id bigint NOT NULL,
                                                          parameter_name character varying(100) NOT NULL,
    parameter_type character varying(100) NOT NULL,
    parameter_value character varying(2500),
    identifying character(1) NOT NULL,
    CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution(job_execution_id)
    );

CREATE TABLE IF NOT EXISTS batch_job_execution_context (
                                                           job_execution_id bigint NOT NULL PRIMARY KEY,
                                                           short_context character varying(2500) NOT NULL,
    serialized_context text,
    CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution(job_execution_id)
    );

CREATE TABLE IF NOT EXISTS batch_step_execution (
                                                    step_execution_id bigint NOT NULL PRIMARY KEY,
                                                    version bigint NOT NULL,
                                                    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    create_time timestamp without time zone,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone,
    CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution(job_execution_id)
    );

CREATE TABLE IF NOT EXISTS batch_step_execution_context (
                                                            step_execution_id bigint NOT NULL PRIMARY KEY,
                                                            short_context character varying(2500) NOT NULL,
    serialized_context text,
    CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES batch_step_execution(step_execution_id)
    );

CREATE TABLE IF NOT EXISTS task_definitions (
                                                definition_name character varying(255) NOT NULL PRIMARY KEY,
    definition text,
    description character varying(255)
    );

CREATE TABLE IF NOT EXISTS task_deployment (
                                               id bigint NOT NULL PRIMARY KEY,
                                               object_version bigint,
                                               task_deployment_id character varying(255) NOT NULL,
    task_definition_name character varying(255) NOT NULL,
    platform_name character varying(255) NOT NULL,
    created_on timestamp without time zone
    );

CREATE TABLE IF NOT EXISTS task_execution (
                                              task_execution_id bigint NOT NULL PRIMARY KEY,
                                              start_time timestamp without time zone,
                                              end_time timestamp without time zone,
                                              task_name character varying(100),
    exit_code integer,
    exit_message character varying(2500),
    error_message character varying(2500),
    last_updated timestamp without time zone,
    external_execution_id character varying(255),
    parent_execution_id bigint
    );

CREATE TABLE IF NOT EXISTS task_execution_metadata (
                                                       id bigint NOT NULL PRIMARY KEY,
                                                       task_execution_id bigint NOT NULL,
                                                       task_execution_manifest text,
                                                       CONSTRAINT task_metadata_fk FOREIGN KEY (task_execution_id) REFERENCES task_execution(task_execution_id)
    );

CREATE TABLE IF NOT EXISTS task_execution_params (
                                                     task_execution_id bigint NOT NULL,
                                                     task_param character varying(2500),
    CONSTRAINT task_exec_params_fk FOREIGN KEY (task_execution_id) REFERENCES task_execution(task_execution_id)
    );

CREATE TABLE IF NOT EXISTS task_lock (
                                         lock_key character(36) NOT NULL,
    region character varying(100) NOT NULL,
    client_id character(36),
    created_date timestamp without time zone NOT NULL,
    CONSTRAINT lock_pk PRIMARY KEY (lock_key, region)
    );

CREATE TABLE IF NOT EXISTS task_task_batch (
                                               task_execution_id bigint NOT NULL,
                                               job_execution_id bigint NOT NULL,
                                               CONSTRAINT task_exec_batch_fk FOREIGN KEY (task_execution_id) REFERENCES task_execution(task_execution_id)
    );

-- Sequences
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS batch_job_execution_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS batch_job_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS batch_step_execution_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS task_execution_metadata_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS task_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS run_id_sequence START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- Implicit indexes
CREATE INDEX IF NOT EXISTS step_name_idx ON batch_step_execution(step_name);
CREATE INDEX IF NOT EXISTS task_execution_id_idx ON task_execution_params(task_execution_id);

-- Explicitly creating indexes, not part of provided schema
CREATE INDEX IF NOT EXISTS job_instance_job_name_idx ON batch_job_instance(job_name);
CREATE INDEX IF NOT EXISTS job_instance_job_key_idx ON batch_job_instance(job_key);
CREATE INDEX IF NOT EXISTS step_execution_version_idx ON batch_step_execution(version);
CREATE INDEX IF NOT EXISTS step_execution_step_name_idx ON batch_step_execution(step_name);
CREATE INDEX IF NOT EXISTS step_execution_job_execution_id_idx ON batch_step_execution(job_execution_id);
