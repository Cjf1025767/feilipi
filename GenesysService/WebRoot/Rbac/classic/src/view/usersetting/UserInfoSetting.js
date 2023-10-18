/**
 * 用户设置
 */
Ext.define('Rbac.view.usersetting.UserInfoSetting', {
    extend: 'Ext.panel.Panel',
    xtype: 'UserInfoSetting',
    requires: [
        'Rbac.view.usersetting.UserInfoSettingController',
        'Rbac.view.usersetting.UserInfoSettingModel',
        'Ext.grid.column.Check',
        'Ext.grid.Panel',
        'Ext.form.Panel',
        'Ext.tree.Panel',
        'Ext.form.*',
        'Ext.grid.filters.*'
    ],
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    controller: 'userinfosetting',
    viewModel: {
        type: 'userinfosetting'
    },
    items: [{
            width: 300,
            xtype: 'treepanel',
            scrollable: true,
            rootVisible: true,
            useArrows: true,
            title: '分组列表',
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
                    var usersgrid = me.up().up().down('#centerId');
                    Ext.apply(usersgrid.store.proxy.extraParams, {
                        roleguid: record.get('id')
                    });
                    usersgrid.store.load();
                    var userpanel = me.up().up().down('#eastId');
                    var form = userpanel.getForm();
                    form.setValues({
                        userguid: ''
                    });
                },
                beforedrop: function (node, data, overModel, dropPosition, dropHandlers) {
                    dropHandlers.wait = true;
                    if (data.records.length > 0) {
                        var usersgrid = this.up().up().down('#centerId');
                        Ext.Ajax.request({
                            url: '/tab/rbac/UIChangeUserRole',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            async: false,
                            params: {
                                userguid: data.records[0].get('userguid'),
                                newroleguid: overModel.data.id,
                                roleguid: usersgrid.store.proxy.extraParams.roleguid
                            },
                            failure: function (response, opts) {
                                dropHandlers.cancelDrop();
                                console.log('server-side failure with status code ' + response.status);
                            },
                            success: function (response, opts) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    usersgrid.store.load();
                                } else {
                                    Ext.Msg.alert("错误", "移动用户失败");
                                }
                                dropHandlers.cancelDrop();
                            }
                        });
                    }
                }
            },
            viewConfig: {
                plugins: {
                    ptype: 'treeviewdragdrop',
                    ddGroup: 'gridtotree',
                    enableDrag: false
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
                        mapping: 'roleguid'
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
                    }
                ],
                folderSort: true,
                sorters: [{
                    property: 'text',
                    direction: 'ASC'
                }]
            }),
            columns: [
                {
                    xtype: 'rownumberer',
                    width: 45,
                    align: 'center'
                },{
                    xtype: 'treecolumn', //this is so we know which column will show the tree
                    text: '角色(组)',
                    dataIndex: 'text',
                    flex: 2,
                    menuDisabled: true,
                    sortable: true
                },
                {
                    xtype: 'actioncolumn',
                    align: 'center',
                    text: '粘贴',
                    tooltip: '粘贴',
                    width: 50,
                    menuDisabled: true,
                    items: [{
                        iconCls: 'x-fa fa-files-o',
                        tooltip: '粘贴',
                        handler: function (view, rowIndex, cellIndex, item, e, record, row) {
                            var usersgrid = view.up().up().down('#centerId');
                            if (usersgrid.store.paste_userguid.length > 0 &&
                                typeof (usersgrid.store.proxy.extraParams.roleguid) != 'undefined') {
                                    var roleguid=record.get('id')
                                    var roleguidgroup = new Array();
                                Ext.Ajax.request({
                                    url: '/tab/rbac/UIAddUserListToTreeNodes',
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    async: true,
                                    params: {
                                       // roleguid: usersgrid.store.proxy.extraParams.roleguid,
                                        roleguid:roleguid,
                                        userguidList: usersgrid.store.paste_userguid
                                    },
                                    failure: function (response, opts) {
                                        console.log('server-side failure with status code ' + response.status);
                                    },
                                    success: function (response, opts) {
                                        var obj = Ext.decode(response.responseText);
                                        if (obj.success) {
                                            if(obj.msg){
                                                Ext.Msg.alert('提示', obj.msg);
                                            }
                                            usersgrid.store.load();
                                        } else {
                                            Ext.Msg.alert('错误', obj.msg);
                                        }
                                    }
                                });
                            } else {
                                Ext.Msg.alert('注意', '需要复制用户后粘贴');
                            }
                        }
                    }]
                }
            ]
        }, {
            xtype: 'gridpanel',
            itemId: 'centerId',
            flex: 1,
            margin: '0 0 0 10',
            title: '用户列表',
            // width: 400,
            scrollable: true,
            selModel: Ext.create('Ext.selection.CheckboxModel', {
                mode: 'SIMPLE',
                checkOnly: true,
                hidden: true
            }),
            store: Ext.create('Ext.data.Store', {
                paste_userguid: new Array(),
                autoload: false,
                proxy: {
                    type: 'rest',
                    url: '/tab/rbac/UIGetUsersTreeNodes',
                    actionMethods: {
                        read: 'POST'
                    },
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    },
                    reader: {
                        type: 'json',
                        rootProperty: 'Users',
                        totalProperty : 'totalCount'
                    }
                },
                fields: [{
                        name: 'nickname'
                    },
                    {
                        name: 'userguid'
                    },
                    {
                        name: 'headimgurl'
                    }
                ]
            }),
            dockedItems: [{
                xtype: 'pagingtoolbar',
                dock: 'bottom',
                pageSize: 10,
                displayInfo: true,
                displayMsg: 'Display data from {0} to {1}, a total of {2}',
                emptyMsg: 'No record',
                listeners: {
                    beforerender: function (bgbar) {
                        bgbar.add('-');
                        bgbar.add(
                            new Ext.form.NumberField({
                                minWidth: 90,
                                maxWidth: 90,
                                minValue: 1,
                                maxValue: 1000,
                                step: 10,
                                value: bgbar.up().store.pageSize,
                                listeners: {
                                    'change': function (field, newValue) {
                                        bgbar.pageSize = newValue;
                                        bgbar.up().store.pageSize = newValue;
                                    }
                                }
                            }));
                        bgbar.add('Records/Page');
                        bgbar.setStore(bgbar.up().store);
                    },
                }
            }],
            tbar: {
                items: [{
                    text: '复制已选',
                    handler: function () {
                        var grid = this.up().up();
                        var selectRecords = grid.getSelection();
                        var recordList=new Array()
                        Ext.Array.each(selectRecords, function (record) {
                            var userguid=record.get("userguid");
                            recordList.push(userguid);
                        });
                        grid.store.paste_userguid =recordList;
                    }
                },
                {
                    text: '删除已选',
                    handler: function () {
                        var grid = this.up().up();
                        var selectRecords = grid.getSelection();
                        var recordList=new Array()
                        Ext.Array.each(selectRecords, function (record) {
                            var userguid=record.get("userguid");
                            recordList.push(userguid);
                        });
                      var  roleguid = typeof (grid.store.proxy.extraParams.roleguid) == 'undefined' ?
                        null :
                        grid.store.proxy.extraParams.roleguid;


                        Ext.Msg.show({
                            title: '删除?',
                            message: '是否从所有组中删除该用户?',
                            buttons: Ext.Msg.YESNOCANCEL,
                            icon: Ext.Msg.QUESTION,
                            fn: Ext.Function.bind(function (btn) {
                                if (btn === 'yes') {
                                    all = true;
                                } else if (btn === 'no') {
                                    all = false
                                } else if (btn == 'cancel') {
                                    return;
                                }
                                if (recordList != null && recordList.length>0 && roleguid != null) {
                                    Ext.Ajax.request({
                                        url: '/tab/rbac/UIRemoveUserList',
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/x-www-form-urlencoded'
                                        },
                                        async: true,
                                        params: {
                                            roleguid:roleguid,
                                            userguidList: recordList,
                                            all: all
                                        },
                                        failure: function (response, opts) {
                                            console.log('server-side failure with status code ' + response.status);
                                        },
                                        success: function (response, opts) {
                                            var obj = Ext.decode(response.responseText);
                                            if (obj.success) {
                                                if(obj.msg){
                                                    Ext.Msg.alert('提示', obj.msg);
                                                }
                                                grid.store.load();
                                            } else {
                                                Ext.Msg.alert('错误', obj.msg);
                                            }
                                        }
                                    });
                                }
                            }, grid)
                        });
                   
                    }
                },
            {
                
                xtype: 'button',
                ui: 'soft-green',
                text: '飞利浦批量新增',
                width: 160,
                handler: function (me) {
              
        var grid = this.up().up();
        Ext.create("Ext.window.Window", {
            title: "批量添加用户",
            width: 480,
            height: 220,
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
                            width: 450,
                            emptyText: '请输入如：123456或者123546，123457或者100000-110000的格式',
                            name: "agentname",
                            fieldLabel: "坐席号和用户姓名",
                            labelWidth: 60,
                            allowBlank: false
                        },
                        {
                            xtype: "combobox",
                            allowBlank: false,
                            name: 'groupname',
                            fieldLabel: "部门名称",
                            forceSelection: true,
                            emptyText: '选择部门',
                            labelWidth: 60,
                            width: 450,
                            triggerAction: 'all',
                            valueField: 'sGroupGuid',
                            displayField: 'sGroupName',
                            store: Ext.create('Ext.data.Store', {
                                fields: [{
                                        name: 'sGroupGuid',
                                        mapping: 'roleguid'
                                    },
                                    {
                                        name: 'sGroupName',
                                        mapping: 'rolename'
                                    }
                                ],
                                proxy: {
                                    type: 'rest',
                                    url: '/tab/rbac/UIGetRolesForRole',
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
                                autoload: true
                            })
                        },
                    ]
                }]
            }],
            buttonAlign: 'right',
            buttons: [{
                xtype: "button",
                text: "确定",
                margin: '0 31 0 0',
                handler: function () {
                    var mee = this;
                    var win = this.up().up();
                    var extension = win.down('textfield[name="agentname"]').getValue();
                    var extensions = extension;
                    var extensiongroup = new Array();
                    var groupguid = win.down('combobox[name="groupname"]').getValue();
                    //判断输入的分机号格式
                    if (
                        typeof (extension) != 'undefined' && extension.length > 0 &&
                        typeof (groupguid) != 'undefined' && groupguid != null) {
                        var rng = /^[0-9]{1,7}-[0-9]{1,7}$/
                        var rrr = /^[0-9]{1,7}(,[0-9]{1,7})*$/
                        if (rng.test(extensions) || rrr.test(extensions)) {
                            var rrg = /,/;
                            var rgg = /-/;
                            //多个分机号逗号隔开格式
                            if (rrg.test(extensions)) {
                                extensiongroup = extension.toString().split(",");
                            }
                            //连续分机号格式
                            else if (rgg.test(extensions)) {
                                var firstextension = extension.toString().split("-")[0];
                                var secondextension = extension.toString().split("-")[1];
                                for (var i = firstextension; i <= secondextension; i++) {
                                    extensiongroup.push(i);
                                }
                            }
                            //单个分机号格式
                            else {
                                extensiongroup.push(extension);
                            }
                            Ext.Ajax.request({
                                url: '/tab/rec/UIAddUsers',
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded'
                                },
                                async: true,
                                params: {
                                    extensionList: extensiongroup,
                                    roleguid: groupguid,
                                 
                                },
                                failure: function (response, opts) {
                                    console.log('server-side failure with status code ' + response.status);
                                },
                                success: function (response, opts) {
                                    var obj = Ext.decode(response.responseText);
                                    if (obj.success) {
                                        mee.up("window").close();
                                        grid.store.load();
                                    } else {
                                        Ext.Msg.alert('请重新输入', '错误: ' + obj.msg);
                                    }
                                }
                            });
                        } else {
                            Ext.Msg.alert('错误', '请输入正确的分机格式');
                            this.up().up().down('textfield[name="extensionname"]').setValue('');
                        }
                    } else {
                        Ext.Msg.alert('错误', '分机号和部门不能为空');
                        this.up().up().down('textfield[name="extensionname"]').setValue('');
                    }
                }
            }]
        }).show();
                }
            
            }
            ]},
            viewConfig: {
                loadMask: true,
                enableTextSelection: true,
                // plugins: [{
                //     ptype: 'gridviewdragdrop',
                //     ddGroup: 'gridtotree',
                //     enableDrop: false
                // }]
            },
            listeners: {
                itemclick: function (me, record, item, index, e, eOpts) {
                    Ext.apply(me.up().store.proxy.extraParams, {
                        userguid: record.get('userguid')
                    });
                    Ext.Ajax.request({
                        url: '/tab/rbac/UIGetUserAuths',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        async: true,
                        params: {
                            userguid: record.get('userguid')
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                var userpanel = me.up().up().down('#eastId');
                                var form = userpanel.getForm();
                                form.setValues(obj);
                                form.setValues({
                                    userguid: record.get('userguid')
                                });
                            }
                        }
                    });
                },
                'edit': function (editor, context, eOpts) {
                    var ctx = context;
                    if(ctx.field=='nickname'){
                        Ext.Ajax.request({
                            url: '/tab/rbac/UIUpdateNickname',
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            params: {
                                userguid: ctx.record.get('userguid'),
                                nickname: ctx.record.get('nickname')
                            },
                            success: function (response, options) {
                                var obj = Ext.decode(response.responseText);
                                if (obj.success) {
                                    ctx.record.commit();
                                } else {
                                    ctx.record.reject();
                                }
                            },
                            failure: function (response, opts) {
                                ctx.record.reject();
                                console.log('server-side failure with status code ' + response.status);
                            }
                        });
                    }
                }
            },
            plugins: ['gridfilters',{
                ptype: 'cellediting',
                clicksToEdit: 2,
            }],
            columns: [
                {
                    xtype: 'rownumberer',
                    width: 45,
                    align: 'center'
                },{
                    width: 80,
                    text: '头像',
                    dataIndex: 'headimgurl',
                    align: 'center',
                    menuDisabled: true,
                    renderer: function (v) {
                        if (v==null || v.length == 0) {
                            return '<img style="width:14px;height:14px" src="/images/weiU.jpg">';
                        }
                        return '<img style="width:14px;height:14px" src="' + v + '">';
                    }
                }, {
                    text: '昵称',
                    width: 80,
                    dataIndex: 'nickname',
                    align: 'center',
                    flex: 1,
                    filter: {
                    	type: 'string'
                    },
                    editor: {
                        allowBlank: false
                    }
                }, {
                    xtype: 'actioncolumn',
                    text: '复制',
                    align: 'center',
                    width: 50,
                    readOnly: true,
                    menuDisabled: true,
                    items: [{
                        text: '复制',
                        tooltip: '复制后的账号可以粘贴到其他组',
                        iconCls: 'x-fa fa-files-o',
                        handler: function (colbtn, rowIndex, cellIndex, item, e, record, row) {
                            var usersgrid = colbtn.up();
                            var arr=new Array();
                            arr.push(record.get('userguid'));
                            usersgrid.store.paste_userguid =arr ;
                        }
                    }]
                },
                {
                    xtype: 'actioncolumn',
                    text: '删除',
                    align: 'center',
                    width: 50,
                    readOnly: true,
                    menuDisabled: true,
                    items: [{
                        text: '删除',
                        tooltip: '删除账号',
                        iconCls: 'x-fa fa-trash',
                        handler: function (colbtn, rowIndex, cellIndex, item, e, record, row) {
                            var userguid = record.get('userguid');
                            var usersgrid = colbtn.up();
                            Ext.Msg.show({
                                title: '删除?',
                                message: '是否从所有组中删除该用户?',
                                buttons: Ext.Msg.YESNOCANCEL,
                                icon: Ext.Msg.QUESTION,
                                fn: Ext.Function.bind(function (btn) {
                                    var roleguid = null,
                                        all = false;
                                    var usersgrid = this;
                                    roleguid = typeof (usersgrid.store.proxy.extraParams.roleguid) == 'undefined' ?
                                        null :
                                        usersgrid.store.proxy.extraParams.roleguid;
                                    if (btn === 'yes') {
                                        all = true;
                                    } else if (btn === 'no') {
                                        all = false
                                    } else if (btn == 'cancel') {
                                        return;
                                    }
                                    if (userguid != null || roleguid != null) {
                                        Ext.Ajax.request({
                                            url: '/tab/rbac/UIRemoveUser',
                                            method: 'POST',
                                            headers: {
                                                'Content-Type': 'application/x-www-form-urlencoded'
                                            },
                                            async: true,
                                            params: {
                                                roleguid: roleguid,
                                                userguid: userguid,
                                                all: all
                                            },
                                            failure: function (response, opts) {
                                                console.log('server-side failure with status code ' + response.status);
                                            },
                                            success: function (response, opts) {
                                                var obj = Ext.decode(response.responseText);
                                                if (obj.success) {
                                                    usersgrid.store.load();
                                                } else {
                                                    Ext.Msg.alert('错误', obj.msg);
                                                }
                                            }
                                        });
                                    }
                                }, usersgrid)
                            });
                        }
                    }]
                }
            ]
        },
        {
            xtype: 'form',
            itemId: 'eastId',
            width: 450,
            title: '修改用户账号信息',
            margin: '0 0 0 10',
            items: [{
                xtype: 'fieldcontainer',
                padding: 10,
                items: [{
                        xtype: 'hiddenfield',
                        name: 'nickname'
                    },
                    {
                        xtype: 'hiddenfield',
                        name: 'userguid'
                    },
                    {
                        xtype: 'hiddenfield',
                        name: 'roleguid'
                    },
                    {
                        xtype: 'fieldcontainer',
                        layout: 'hbox',
                        defaults: {
                            margin: '0 0 0 10'
                        },
                        items: [{
                                xtype: 'textfield',
                                fieldLabel: '姓名',
                                name: 'username',
                                width: 200,
                                labelWidth: 45
                            },
                            {
                                xtype: 'textfield',
                                fieldLabel: '工号',
                                name: 'agent',
                                width: 200,
                                labelWidth: 45
                            }
                        ]
                    },
                    {
                        xtype: 'fieldcontainer',
                        layout: 'hbox',
                        defaults: {
                            margin: '0 0 0 10'
                        },
                        items: [{
                                xtype: 'textfield',
                                fieldLabel: '密码',
                                name: 'password',
                                inputType: 'password',
                                width: 200,
                                labelWidth: 45
                            },
                            {
                                xtype: 'textfield',
                                fieldLabel: '分机',
                                name: 'mobile',
                                width: 200,
                                labelWidth: 45
                            }
                        ]
                    },
                    {
                        xtype: 'textfield',
                        margin: '0 0 0 10',
                        fieldLabel: '员工号',
                        name: 'email',
                        width: 410,
                        labelWidth: 45
                    },
                    {
                        xtype: 'textfield',
                        margin: '10 0 0 10',
                        fieldLabel: '身份证',
                        name: 'identifier',
                        width: 410,
                        labelWidth: 45
                    },
                    {
                        xtype: 'toolbar',
                        width: 429,
                        items: [
                            {
                                xtype: 'button',
                                ui: 'soft-green',
                                text: '新增',
                                width: 80,
                                handler: function (me) {
                                    var usersgrid = me.up().up().up().up().down('#centerId');
                                    if (typeof (usersgrid.store.proxy.extraParams.roleguid) != 'undefined') {
                                        var form = me.up().up().up();
                                        var values = form.getValues();
                                        var nickname = values.username;
                                        if(values.agent!=null && values.agent.length>0){
                                            nickname += "(" + values.agent + ")";
                                        }
                                        Ext.Msg.prompt('昵称', '请输入新增用户的昵称:', function (btn, text) {
                                            if (btn == 'ok') { 
                                                values.nickname = text;
                                                values.userguid = "";
                                                values.roleguid = usersgrid.store.proxy.extraParams.roleguid;
                                                if (typeof (values.password) != 'undefined' && values.password.length > 0) {
                                                    values.password = globalVars.base64 ? window.btoa(values.password) : md5(values.password);
                                                    form.getForm().setValues(values);
                                                } else {
                                                    form.getForm().setValues(values);
                                                }
                                                form.submit({
                                                    url: '/tab/rbac/UIAddNewUser',
                                                    success: function (form, action) {
                                                        form.setValues({
                                                            password: ''
                                                        });
                                                        usersgrid.store.reload();
                                                    },
                                                    failure: function (form, action) {
                                                        switch (action.failureType) {
                                                            case Ext.form.action.Action.CLIENT_INVALID:
                                                                Ext.Msg.alert('失败', '您输入的某些内容无效');
                                                                break;
                                                            case Ext.form.action.Action.CONNECT_FAILURE:
                                                                Ext.Msg.alert('失败', '网络通讯错误');
                                                                break;
                                                            case Ext.form.action.Action.SERVER_INVALID:
                                                                Ext.Msg.alert('失败', action.result.msg);
                                                                break;
                                                        }
                                                    }
                                                });
                                            }
                                        },null,false,nickname,{
                                            autoCapitalize: true,
                                            placeHolder: '空则默认-姓名(工号)'
                                        });
                                    }
                                }
                            }, '->',
                            
                            {
                                xtype: 'button',
                                text: '保存',
                                width: 80,
                                handler: function (me) {
                                    var form = me.up().up().up();
                                    var values = form.getValues();
                                    if (typeof (values.userguid) == 'undefined' || values.userguid.length == 0) {
                                        Ext.Msg.alert('失败', '请选择修改的用户');
                                        return;
                                    }
                                    if (typeof (values.password) != 'undefined' && values.password.length > 0) {
                                        values.password = globalVars.base64 ? window.btoa(values.password) : md5(values.password);
                                        form.getForm().setValues(values);
                                    }
                                    form.submit({
                                        url: '/tab/rbac/UIUpdateUser',
                                        success: function (form, action) {
                                            form.setValues({
                                                password: ''
                                            });
                                        },
                                        failure: function (form, action) {
                                            form.setValues({
                                                password: ''
                                            });
                                            switch (action.failureType) {
                                                case Ext.form.action.Action.CLIENT_INVALID:
                                                    Ext.Msg.alert('失败', '您输入的某些内容无效');
                                                    break;
                                                case Ext.form.action.Action.CONNECT_FAILURE:
                                                    Ext.Msg.alert('失败', '网络通讯错误');
                                                    break;
                                                case Ext.form.action.Action.SERVER_INVALID:
                                                    Ext.Msg.alert('失败', action.result.msg);
                                                    break;
                                            }
                                        }
                                    });
                                }
                            }
                        ]
                    }
                ]
            }]
        }
    ]
});