INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, url, target, menu_type, visible, perms, icon, create_by, create_time, remark)
VALUES
    (2000, '数字孪生', 0, 5, '#', '', 'M', '0', NULL, 'fa fa-cube', 'admin', now(), '数字孪生目录'),
    (2001, '三维底座', 2000, 1, '/twin/index', 'menuItem', 'C', '0', 'twin:scene:view', 'fa fa-globe', 'admin', now(), ''),
    (2002, '建筑管理', 2000, 2, '/twin/building', 'menuItem', 'C', '0', 'twin:building:view', 'fa fa-building', 'admin', now(), ''),
    (2003, '设备管理', 2000, 3, '/twin/device', 'menuItem', 'C', '0', 'twin:device:view', 'fa fa-microchip', 'admin', now(), ''),
    (2004, '告警记录', 2000, 4, '/twin/alarm', 'menuItem', 'C', '0', 'twin:alarm:view', 'fa fa-bell', 'admin', now(), '');

SELECT setval('sys_menu_menu_id_seq', (SELECT max(menu_id) FROM sys_menu));
