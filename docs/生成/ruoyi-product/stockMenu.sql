-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('库存管理', '2003', '1', 'stock', 'mall-product/stock/index', 1, 0, 'C', '0', '0', 'mall-product:stock:list', '#', 'admin', sysdate(), '', null, '库存管理菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('库存管理查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:stock:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('库存管理新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:stock:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('库存管理修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:stock:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('库存管理删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:stock:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('库存管理导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:stock:export',       '#', 'admin', sysdate(), '', null, '');