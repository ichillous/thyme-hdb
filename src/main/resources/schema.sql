-- schema.sql (fixed)
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS USER_ACCOUNT;

CREATE TABLE USER_ACCOUNT (
                              user_id    VARCHAR(36) DEFAULT CAST(RANDOM_UUID() AS VARCHAR(36)) PRIMARY KEY,
                              email      VARCHAR(120),
                              username   VARCHAR(120),
                              phone      VARCHAR(20),
                              created_at TIMESTAMP,
                              updated_at TIMESTAMP
);

ALTER TABLE USER_ACCOUNT ADD CONSTRAINT uq_user_email    UNIQUE (email);
ALTER TABLE USER_ACCOUNT ADD CONSTRAINT uq_user_username UNIQUE (username);
ALTER TABLE USER_ACCOUNT ADD CONSTRAINT uq_user_phone    UNIQUE (phone);

CREATE TABLE user_roles (
                            user_id VARCHAR(36) NOT NULL,
                            role    VARCHAR(50) NOT NULL,
                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id) REFERENCES USER_ACCOUNT(user_id) ON DELETE CASCADE
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
-- ===== EVENT =====
create table if not exists EVENT (
                                     event_id        varchar(40)  primary key,
                                     owner_org_id    varchar(40)  not null,
                                     title           varchar(160) not null,
                                     description     varchar(4000),
                                     venue_name      varchar(200),
                                     address_line    varchar(300),
                                     city            varchar(120),
                                     state_province  varchar(2),
                                     postal_code     varchar(40),
                                     tz              varchar(60),
                                     start_at        timestamp    not null,
                                     end_at          timestamp    not null,
                                     published       boolean      not null default true,
                                     created_at      timestamp    default current_timestamp(),
                                     updated_at      timestamp
);

create index if not exists idx_event_owner on EVENT(owner_org_id);
create index if not exists idx_event_start on EVENT(start_at);
create index if not exists idx_event_end   on EVENT(end_at);

-- FK to USER_ACCOUNT (assumes USER_ACCOUNT.user_id exists)
alter table EVENT
    add constraint if not exists fk_event_owner
    foreign key (owner_org_id) references USER_ACCOUNT(user_id);

-- ===== EVENT_HEART (many-to-many) =====
create table if not exists EVENT_HEART (
                                           event_id varchar(40) not null,
                                           user_id  varchar(40) not null,
                                           created_at timestamp default current_timestamp(),
                                           primary key (event_id, user_id),
                                           constraint fk_heart_event foreign key (event_id) references EVENT(event_id) on delete cascade,
                                           constraint fk_heart_user  foreign key (user_id)  references USER_ACCOUNT(user_id) on delete cascade
);

create index if not exists idx_heart_user on EVENT_HEART(user_id);

-- ===== PROFILE =====
create table if not exists PROFILE (
                                       profile_id           varchar(40)  primary key,
                                       user_id              varchar(40)  not null unique,
                                       display_name         varchar(160),
                                       bio                  varchar(4000),
                                       public_profile       boolean      not null default false,

                                       website              varchar(300),
                                       contact_email        varchar(200),
                                       contact_phone        varchar(40),
                                       logo_url             varchar(500),
                                       banner_url           varchar(500),

                                       address_line1        varchar(300),
                                       address_line2        varchar(300),
                                       city                 varchar(120),
                                       state_province       varchar(2),
                                       postal_code          varchar(40),
                                       country_code         varchar(2)  not null default 'US',

                                       org_type             varchar(40)  not null default 'NONE',

                                       programs_offered     varchar(4000),
                                       classes_offered      varchar(4000),

                                       prayer_times_mode    varchar(40)  not null default 'DISABLED',
                                       prayer_times_last_updated timestamp,

                                       donation_enabled     boolean      not null default false,
                                       donation_provider    varchar(80),
                                       donation_url         varchar(500),

                                       created_at           timestamp default current_timestamp(),
                                       updated_at           timestamp,

                                       constraint fk_profile_user foreign key (user_id)
                                           references USER_ACCOUNT(user_id)
);

create index if not exists idx_profile_public on PROFILE(public_profile);
create index if not exists idx_profile_orgtype on PROFILE(org_type);

-- Services (enum list)
create table if not exists PROFILE_SERVICES (
                                                profile_id   varchar(40) not null,
                                                service_name varchar(80) not null,
                                                primary key (profile_id, service_name),
                                                constraint fk_ps_profile foreign key (profile_id) references PROFILE(profile_id) on delete cascade
);

-- Weekly prayer times (rows per day)
create table if not exists PROFILE_PRAYER_TIME (
                                                   id           varchar(40) primary key,
                                                   profile_id   varchar(40) not null,
                                                   day_of_week  int         not null, -- 1-7 (Mon..Sun)
                                                   fajr         time,
                                                   dhuhr        time,
                                                   asr          time,
                                                   maghrib      time,
                                                   isha         time,
                                                   constraint uq_profile_day unique (profile_id, day_of_week),
                                                   constraint fk_ppt_profile foreign key (profile_id) references PROFILE(profile_id) on delete cascade
);

-- Multiple Jummah entries
create table if not exists PROFILE_JUMMAH_TIME (
                                                   id           varchar(40) primary key,
                                                   profile_id   varchar(40) not null,
                                                   start_time   time,
                                                   notes        varchar(300),
                                                   constraint fk_pjt_profile foreign key (profile_id) references PROFILE(profile_id) on delete cascade
);

create index if not exists idx_jummah_profile on PROFILE_JUMMAH_TIME(profile_id);


-- Helpful indexes for city/state filtering
create index if not exists idx_event_city on EVENT(city);
create index if not exists idx_event_city_state on EVENT(city, state_province);
