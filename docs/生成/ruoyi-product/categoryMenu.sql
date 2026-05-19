-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('商品类目', '2003', '1', 'category', 'mall-product/category/index', 1, 0, 'C', '0', '0', 'mall-product:category:list', '#', 'admin', sysdate(), '', null, '商品类目菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('商品类目查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:category:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('商品类目新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:category:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('商品类目修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:category:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('商品类目删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:category:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('商品类目导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mall-product:category:export',       '#', 'admin', sysdate(), '', null, '');