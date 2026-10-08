-- 用户上传头像：存放 adi_file.uuid；空表示使用系统生成头像
ALTER TABLE adi_user ADD COLUMN IF NOT EXISTS avatar_file_uuid varchar(32) NOT NULL DEFAULT '';
COMMENT ON COLUMN adi_user.avatar_file_uuid IS 'Uploaded avatar file uuid (adi_file.uuid); empty = system generated avatar';
