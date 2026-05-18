-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('用户账号', '2002', '1', 'user', 'mall-user/user/index', 1, 0, 'C', '0', '0', 'mall-user:user:list', '#', 'admin', sysdate(), '', null, '用户账号菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('用户账号查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:user:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('用户账号新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:user:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('用户账号修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:user:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('用户账号删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:user:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('用户账号导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:user:export',       '#', 'admin', sysdate(), '', null, '');