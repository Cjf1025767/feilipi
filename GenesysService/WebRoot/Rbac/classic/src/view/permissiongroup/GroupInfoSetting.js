/**
 * 分组设置
 */
Ext.define('Rbac.view.permissiongroup.GroupInfoSetting', {
    extend: 'Ext.panel.Panel',
    xtype: 'groupInfoSetting',
    requires: [
        'Rbac.view.permissiongroup.GroupInfoSettingController',
        'Rbac.view.permissiongroup.GroupInfoSettingModel',
        'Ext.grid.column.Check',
        'Ext.grid.Panel',
        'Ext.grid.CellEditor',
        'Ext.tree.Panel',
        'Ext.form.*'
    ],
    controller: 'groupinfosetting',
    viewModel: {
        type: 'groupinfosetting'
    },
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    items: [{
        width: 500,
        xtype: 'treepanel',
        title: '分组列表',
        scrollable: true,
        rootVisible: true,
        useArrows: true,
        listeners: {
            afterrender: function (tree) {
                var treepanel = tree;
                tree.store.load({callback: function(records, operation, success) {
                    var record = treepanel.store.getNodeById('9A611B6F-5664-4C43-9D06-C1E2141CCCB1');
                    treepanel.getSelectionModel().select(record);
                    treepanel.fireEvent('itemclick',treepanel.getView(),record);
                }});
            },
            itemclick: function (me, record, item, index, e, eOpts) {
                var treepanel = me.up();
                var fatherroleguid = treepanel.store.proxy.extraParams.roleguid;
                if (typeof (fatherroleguid) != 'undefined' && fatherroleguid.length > 0 && fatherroleguid === record.get('id')) {} else {
                    Ext.apply(treepanel.store.proxy.extraParams, {
                        roleguid: record.get('id')
                    });
                    treepanel.up().down('#centerId').store.load({
                        params: {
                            roleguid: record.get('id')
                        }
                    });
                }
            },
            beforedrop: function (node, data, overModel, dropPosition, dropHandlers) {
                dropHandlers.wait = true;
                if (data.records.length > 0) {
                    var treepanel = this;
                    var reload = overModel.data.leaf;
                    if (!reload) {
                        var node = treepanel.store.getNodeById(data.records[0].get('fatherroleguid'));
                        if (node.childNodes.length == 1) reload = true;
                    }
                    Ext.Ajax.request({
                        url: '/tab/rbac/UIChangeFatherRole',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        async: false,
                        params: {
                            roleguid: data.records[0].get('roleguid'),
                            fatherroleguid: overModel.data.id
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                dropHandlers.processDrop();
                                if (reload) {
                                    treepanel.store.load();
                                }
                            } else {
                                dropHandlers.cancelDrop();
                            }
                        }

                    });
                } else {
                    dropHandlers.cancelDrop();
                }
            }
        },
        viewConfig: {
            plugins: {
                ptype: 'treeviewdragdrop',
                containerScroll: true
            }
        },
        store: Ext.create('Ext.data.TreeStore', {
            autoLoad: false,
            parentIdProperty: 'fatherroleguid',
            proxy: {
                type: 'ajax',
                url: '/tab/rbac/UIGetRolesTreeNodes',
                actionMethods: {
                    read: 'POST'
                },
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                reader: {
                    type: 'json',
                    rootProperty: 'Roles'
                }
            },
            root: {
                text: '权限系统',
                iconCls: 'x-fa fa-home',
                id: '9A611B6F-5664-4C43-9D06-C1E2141CCCB1',
                expanded: true
            },
            fields: [{
                    name: 'id',
                    mapping: 'roleguid',
                    checked: true
                },
                {
                    name: 'fatherroleguid'
                },
                {
                    name: 'text',
                    mapping: 'rolename'
                },
                {
                    name: 'leaf',
                    convert: function (v, r) {
                        var childrens = r.get('childrens');
                        if (typeof (childrens) == 'undefined') return false; //Root
                        if (childrens != null && childrens.length > 0) {
                            return false;
                        }
                        return true;
                    }
                },
                {
                    name: 'inheritance'
                }
            ],
            folderSort: true,
            sorters: [{
                property: 'text',
                direction: 'ASC'
            }]
        }),
        tbar: {
            items: [{
                    text: '添加',
                    handler: function () {
                        var grid = this.up().up();
                        Ext.create("Ext.window.Window", {
                            title: "添加分组",
                            width: 320,
                            height: 180,
                            resizable: false,
                            bodyPadding: '5 0 0 5',
                            items: [{
                                xtype: "form",
                                defaults: {
                                    anchor: '100%'
                                },
                                fieldDefaults: {
                                    labelAlign: "left",
                                    flex: 1,
                                    margin: 5
                                },
                                items: [{
                                    xtype: "container",
                                    layout: "vbox",
                                    items: [{
                                            xtype: "textfield",
                                            name: "rolename",
                                            fieldLabel: "分组名称",
                                            labelWidth: 60,
                                            allowBlank: false
                                        },
                                        {
                                        xtype: "container",
                                        layout: "hbox",
                                        items: [
                                            {
                                                xtype: "checkboxfield",
                                                name: "inheritance",
                                                boxLabel: "是否继承",
                                                labelWidth: 60,
                                                value:false
                                            },
                                            {
                                                xtype: "checkboxfield",
                                                name: "inheritance_skill",
                                                boxLabel: "技能组",
                                                labelWidth: 60,
                                                value:false
                                            }
                                        ]}
                                        
                                    ]
                                }]
                            }],
                            buttonAlign: 'right',
                            buttons: [{
                                xtype: "button",
                                text: "确定",
                                margin: '0 31 0 0',
                                handler: function () {
                                    var me = this,
                                        fatherroleguid = grid.store.proxy.extraParams.roleguid,
                                        rolename = this.up().up().down('textfield[name="rolename"]').getValue(),
                                        inheritance = this.up().up().down('checkboxfield[name="inheritance"]').getValue(),
                                        inheritance_skill = this.up().up().down('checkboxfield[name="inheritance_skill"]').getValue();
                                    if (typeof (fatherroleguid) != 'undefined' && fatherroleguid.length > 0) {
                                        var target = grid.store.getNodeById(fatherroleguid) || this.getRootNode();
                                        Ext.Ajax.request({
                                            url: '/tab/rbac/UIAddRoleToTreeNodes',
                                            method: 'POST',
                                            headers: {
                                                'Content-Type': 'application/x-www-form-urlencoded'
                                            },
                                            async: true,
                                            params: {
                                                rolename: rolename,
                                                inheritance: inheritance,
                                                fatherroleguid: fatherroleguid,
                                                inheritance_skill:inheritance_skill
                                            },
                                            failure: function (response, opts) {
                                                console.log('server-side failure with status code ' + response.status);
                                            },
                                            success: function (response, opts) {
                                                var obj = Ext.decode(response.responseText);
                                                if (obj.success) {                                      
                                                	grid.getStore().load();
                                                    me.up("window").close();
                                                } else {
                                                    Ext.Msg.alert('错误', '请重新输入');
                                                    me.up().up().down('textfield[name="rolename"]').setValue('');
                                                    me.up().up().down('checkboxfield[name="inheritance"]').setValue('');
                                                }

                                            }

                                        });
                                    } else {
                                        Ext.Msg.alert('错误', '请重新输入');
                                        me.up().up().down('textfield[name="rolename"]').setValue('');
                                        me.up().up().down('checkboxfield[name="inheritance"]').setValue('');
                                    }
                                }
                            }]
                        }).show();
                    }
                }, {
                    text: '删除',
                    handler: function () {
                        var grid = this.up().up();
                        Ext.Msg.confirm('删除', '确定要删除吗?', function (btn) {
                            if (btn == 'yes') {
                            	var node = grid.store.findRecord('roleguid',grid.store.proxy.extraParams.roleguid);
                                Ext.Ajax.request({
                                    url: '/tab/rbac/UIRemoveRole',
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    params: {
                                        roleguid: grid.store.proxy.extraParams.roleguid
                                    },
                                    failure: function (response, opts) {
                                        console.log('server-side failure with status code ' + response.status);
                                    },
                                    success: function (response, opts) {
                                        var obj = Ext.decode(response.responseText);
                                        if (obj.success) {
                                        	var parentNode = grid.store.findRecord('roleguid',node.get('fatherroleguid'));
                                        	if(parentNode){
                                        		parentNode.removeChild(node);
                                        	}
                                        } else {
                                            Ext.Msg.alert("错误", obj.msg);
                                        }
                                    }
                                });
                            }
                        });
                    }
                },
                {
                    text: '鉴权',
                    handler: function () {
                        var grid = this.up().up();
                        Ext.Ajax.request({
                            url: '/tab/rbac/UIGetAppInfo',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                roleguid: grid.store.proxy.extraParams.roleguid
                            },
                            failure: function (response, opts) {
                                console.log('server-side failure with status code ' + response.status);
                            },
                            success: function (response, opts) {
                                var obj = Ext.decode(response.responseText);
                                if (!obj.success) {
                                    Ext.Msg.alert('错误', obj.msg);
                                    return;
                                }
                                var win = Ext.create("Ext.window.Window", {
                                    title: "APP鉴权信息",
                                    width: 430,
                                    height: 268,
                                    resizable: false,
                                    modal: true,
                                    bodyPadding: '5 0 0 5',
                                    items: [{
                                        xtype: "form",
                                        defaults: {
                                            anchor: '100%'
                                        },
                                        fieldDefaults: {
                                            labelAlign: "left",
                                            flex: 1,
                                            margin: 5
                                        },
                                        items: [{
                                            xtype: "container",
                                            layout: "vbox",
                                            items: [{
                                                    xtype: "textfield",
                                                    name: 'appid',
                                                    width: 350,
                                                    fieldLabel: "APP标识",
                                                    labelWidth: 70,
                                                    readOnly: true
                                                },
                                                {
                                                    xtype: "textfield",
                                                    name: 'createdate',
                                                    width: 350,
                                                    fieldLabel: "创建时间",
                                                    labelWidth: 70,
                                                    readOnly: true
                                                },
                                                {
                                                    xtype: "textfield",
                                                    name: 'updatedate',
                                                    width: 350,
                                                    fieldLabel: "更新时间",
                                                    labelWidth: 70,
                                                    readOnly: true
                                                },
                                                {
                                                    xtype: "textfield",
                                                    name: "redirect_uri",
                                                    width: 350,
                                                    labelWidth: 70,
                                                    fieldLabel: "默认值"
                                                },
                                                {
                                                    xtype: "fieldcontainer",
                                                    layout: "hbox",
                                                    items: [{
                                                            xtype: "textfield",
                                                            name: "secret",
                                                            width: 350,
                                                            labelWidth: 70,
                                                            fieldLabel: "安全码",
                                                            margin: 0,
                                                            readOnly: true
                                                        },
                                                        {
                                                            xtype: "button",
                                                            text: "更新",
                                                            margin: "0 0 0 5",
                                                            handler: function () {
                                                                var me = this;
                                                                Ext.Msg.show({
                                                                    title: '注意',
                                                                    message: '更新安全码(Secret)后, 所有APP调用都需要使用新的安全码(Secret)<br />是否更新?',
                                                                    buttons: Ext.Msg.YESNO,
                                                                    icon: Ext.Msg.QUESTION,
                                                                    fn: function (btn) {
                                                                        if (btn === 'yes') {
                                                                            Ext.Ajax.request({
                                                                                url: '/tab/rbac/UIUpdateAppInfo',
                                                                                method: 'POST',
                                                                                headers: {
                                                                                    'Content-Type': 'application/x-www-form-urlencoded'
                                                                                },
                                                                                params: {
                                                                                    roleguid: grid.store.proxy.extraParams.roleguid,
                                                                                    redirect_uri: me.up().up().down("textfield[name=redirect_uri]").getValue()
                                                                                },
                                                                                failure: function (response, opts) {
                                                                                    console.log('server-side failure with status code ' + response.status);
                                                                                },
                                                                                success: function (response, opts) {
                                                                                    var obj = Ext.decode(response.responseText);
                                                                                    if (obj.success) {
                                                                                        me.up().up().down("textfield[name=secret]").setValue(obj.secret);
                                                                                    } else {
                                                                                        Ext.Msg.alert('错误', obj.msg);
                                                                                    }
                                                                                }
                                                                            });
                                                                        } else if (btn === 'no') {
                                                                            console.log('No pressed');
                                                                        } else {
                                                                            console.log('Cancel pressed');
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    ]
                                                }
                                            ]
                                        }]
                                    }]
                                }).show();
                                if (obj.success) {
                                    win.down("textfield[name=appid]").setValue(obj.appid);
                                    win.down("textfield[name=createdate]").setValue(Ext.Date.format(new Date(obj.createdate), 'Y-m-d H:i:s'));
                                    win.down("textfield[name=updatedate]").setValue(Ext.Date.format(new Date(obj.updatedate), 'Y-m-d H:i:s'));
                                    win.down("textfield[name=secret]").setValue(obj.secret);
                                    win.down("textfield[name=redirect_uri]").setValue(obj.redirect_uri);
                                }
                            }
                        });
                    }
                },
                {
                    text: '刷新',
                    handler: function () {
                        var grid = this.up().up();
                        grid.getStore().load();
                    }
                }
            ]
        },
        columns: [{
            xtype: 'treecolumn', //this is so we know which column will show the tree
            text: '角色(组)',
            dataIndex: 'text',
            flex: 2,
            sortable: true,
            editor: {
                xtype: 'textfield',
                allowBlank: false
            }
        }, {
            xtype: 'checkcolumn', //this is so we know which column will show the tree
            text: '技能组',
            sortable: true,
            dataIndex: 'inheritance',
            width: 60,
            readOnly:true,
            renderer:function(val){
                return (new Ext.grid.column.CheckColumn).renderer(val&2);
            },
            listeners: {
                checkchange: function (me, rowIndex, checked, record, e, eOpts) {
                    var r = record;
                    Ext.Ajax.request({
                        url: '/tab/rbac/UIUpdateRoleInfo',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            roleguid: record.get('id'),
                            inheritance_skill: checked
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                r.commit();
                            }
                        }
                    });
                }
            }
        },{
            xtype: 'checkcolumn', //this is so we know which column will show the tree
            text: '继承',
            sortable: true,
            dataIndex: 'inheritance',
            width: 60,
            renderer:function(val){
                return (new Ext.grid.column.CheckColumn).renderer(val&1);
            },
            listeners: {
                checkchange: function (me, rowIndex, checked, record, e, eOpts) {
                    var r = record;
                    Ext.Ajax.request({
                        url: '/tab/rbac/UIUpdateRoleInfo',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            roleguid: record.get('id'),
                            inheritance: checked
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                r.commit();
                            }
                        }
                    });
                }
            }
        }],
        plugins: [
            Ext.create('Ext.grid.plugin.CellEditing', {
                clicksToEdit: 2,
                listeners: {
                    edit: function (me, e) {
                        if (e.field == 'text') {
                            var originalValue = e.originalValue;
                            Ext.Ajax.request({
                                url: '/tab/rbac/UIUpdateRoleInfo',
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                params: {
                                    roleguid: e.record.get('id'),
                                    rolename: e.record.get('text')
                                },
                                failure: function (response, opts) {
                                    console.log('server-side failure with status code ' + response.status);
                                },
                                success: function (response, opts) {
                                    var obj = Ext.decode(response.responseText);
                                    if (!obj.success) {
                                        var node = me.view.store.getNodeById(e.record.get('id'));
                                        node.set('text', originalValue)
                                    }
                                }
                            });
                        }
                    }
                }
            })
        ]
    }, {
        xtype: 'gridpanel',
        title: '权限信息',
        itemId: 'centerId',
        flex: 1,
        margin: '0 0 0 10',
        scrollable: true,
        store: Ext.create('Ext.data.Store', {
            autoload: false,
            proxy: {
                type: 'rest',
                url: '/tab/rbac/UIGetOperationInfos',
                actionMethods: {
                    read: 'POST'
                },
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                reader: {
                    type: 'json',
                    rootProperty: 'OperationInfos'
                }
            },
            fields: []
        }),
        listeners: {
            beforerender: function () {
                this.store.load();
            }
        },
        columns: [{
            text: '名称',
            flex: 2,
            dataIndex: 'operationname'
        }, {
            xtype: 'checkcolumn',
            text: '拥有权限',
            width: 80,
            align: 'center',
            dataIndex: 'checked',
            listeners: {
                checkchange: function (me, rowIndex, checked, record, e, eOpts) {
                	var r = record;
                    var grid = me.up().up();
                    var roleguid = me.up().up().up().down('treepanel').store.proxy.extraParams.roleguid;
                    Ext.Ajax.request({
                        url: '/tab/rbac/UIUpdateRoleOperation',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            roleguid: roleguid,
                            operationguid: record.get('operationguid'),
                            checked: checked
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                r.commit();
                            } else {
                                grid.store.reload({
                                    params: {
                                        roleguid: roleguid
                                    }
                                });
                                Ext.Msg.alert("错误", obj.msg);
                            }
                        }
                    });
                }
            }
        }]
    }]
});