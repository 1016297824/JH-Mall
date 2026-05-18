-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('会员等级定义', '2002', '1', 'level', 'mall-user/level/index', 1, 0, 'C', '0', '0', 'mall-user:level:list', '#', 'admin', sysdate(), '', null, '会员等级定义菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('会员等级定义查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:level:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('会员等级定义新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:level:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('会员等级定义修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:level:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('会员等级定义删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:level:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('会员等级定义导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-user:level:export',       '#', 'admin', sysdate(), '', null, '');