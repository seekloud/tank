--定义数据表

--


CREATE TABLE game_record(
record_id SERIAL8 PRIMARY KEY NOT NULL ,
room_id BIGINT NOT NULL ,
start_time BIGINT NOT NULL ,
end_time BIGINT NOT NULL ,
file_path TEXT NOT NULL
);


create TABLE user_record_map(
user_id BIGINT NOT NULL ,
record_id BIGINT NOT NULL ,
room_id BIGINT NOT NULL
);