-- 初始管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO `user` (`username`, `password_hash`, `role`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');

-- 默认分类
INSERT INTO `category` (`name`, `parent_id`, `sort_order`) VALUES
('角色模型', NULL, 1),
('场景模型', NULL, 2),
('道具模型', NULL, 3),
('建筑模型', NULL, 4),
('交通工具', NULL, 5);

-- 默认标签
INSERT INTO `tag` (`name`) VALUES
('高精度'),
('低模'),
('PBR'),
('手绘风格'),
('动画'),
(' rigged'),
('静态');
