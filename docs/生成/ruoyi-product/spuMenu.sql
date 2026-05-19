-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SPU 管理', '2003', '1', 'spu', 'mall-product/spu/index', 1, 0, 'C', '0', '0', 'mall-product:spu:list', '#', 'admin', sysdate(), '', null, 'SPU 管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SPU 管理查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:spu:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SPU 管理新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:spu:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SPU 管理修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:spu:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SPU 管理删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:spu:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('SPU 管理导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:spu:export',       '#', 'admin', sysdate(), '', null, '');