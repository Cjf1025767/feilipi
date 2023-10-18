Ext.define('Rbac.view.extensetting.extension', {
    extend: 'Ext.panel.Panel',
    xtype: 'settingextensionview',
    requires: [
        'Rbac.view.extensetting.extensionController',
        'Rbac.view.extensetting.extensionModel',
        'Ext.Img',
        'Ext.window.Window',
        'Ext.Component'
    ],

    controller: 'extensetting-extension',
    viewModel: {
        type: 'extensetting-extension'
    },
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    listeners: {
        beforerender: function (view) {
            var store = view.down('gridpanel[name="增删分机"]').getStore();
            store.load();
        }
    },
    items: [{
        width: 450,
        xtype: 'gridpanel',
        name: '增删分机',
        title: '增删分机',
        scrollable: true,
        rootVisible: true,
        useArrows: true,
        selModel: Ext.create('Ext.selection.CheckboxModel', {
            mode: 'SIMPLE',
            checkOnly: true,
            hidden: true
        }),
        plugins: [{
            ptype: 'cellediting',
            clicksToEdit: 2,
        }],
        listeners: {
            edit: function (editor, context, eOpts) {
                var ctx = context;
                if (ctx.field == 'rolename') {
                    var originalValue = ctx.originalValue;
                    var newValue = ctx.record.get('rolename');
                    var extensiongroup = new Array();
                    extensiongroup.push(ctx.record.get('extension'));
                    Ext.Ajax.request({
                        url: '/tab/rec/UIUpdateRecExtension',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        async: true,
                        params: {
                            extensionList: extensiongroup,
                            roleguid: newValue
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                ctx.grid.getStore().reload();
                            }
                        }
                    });
                }else if(ctx.field == 'ipaddress'){
                    var originalValue = ctx.originalValue;
                    var newValue = ctx.record.get('ipaddress');
                    var extensiongroup = new Array();
                    extensiongroup.push(ctx.record.get('extension'));
                    Ext.Ajax.request({
                        url: '/tab/rec/UIUpdateRecExtension',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        async: true,
                        params: {
                            extensionList: extensiongroup,
                            ipaddress: newValue
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                ctx.grid.getStore().reload();
                            }
                        }
                    });
                }else if(ctx.field == 'policy'){
                    var originalValue = ctx.originalValue;
                    var newValue = ctx.record.get('policy');
                    var extensiongroup = new Array();
                    extensiongroup.push(ctx.record.get('extension'));
                    Ext.Ajax.request({
                        url: '/tab/rec/UIUpdateRecExtension',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        async: true,
                        params: {
                            extensionList: extensiongroup,
                            policy: newValue
                        },
                        failure: function (response, opts) {
                            console.log('server-side failure with status code ' + response.status);
                        },
                        success: function (response, opts) {
                            var obj = Ext.decode(response.responseText);
                            if (obj.success) {
                                ctx.grid.getStore().reload();
                            }
                        }
                    });
                }
            }
        },
        columns: [{
			xtype: 'rownumberer',
			width: 45,
			align: 'center'
		},{
            header: "分机号",
            width: 80,
            dataIndex: 'extension',
            menuDisabled: true,
            align: 'center',

        }, {
            header: "地址",
            width: 90,
            menuDisabled: true,
            dataIndex: 'ipaddress',
            align: 'center',
            //editor: {
            //    allowBlank: false
            //}
        }, {
            header: "部门",
            flex: 1,
            menuDisabled: true,
            dataIndex: 'rolename',
            align: 'center',
            editor: {
                xtype: "combobox",
                allowBlank: false,
                name: 'groupname',
                fieldLabel: "部门名称",
                hideLabel:true,
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
            renderer:function(v){
                const regexExp = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/gi;
                if(regexExp.test(v)){
                    return "";
                }
                return v;
            }
        },{
            header: "策略",
            width: 50,
            menuDisabled: true,
            dataIndex: 'policy',
            align: 'center',
            //editor: {
            //    allowBlank: false
            //}
        }/*, {
            xtype: 'actioncolumn',
            text: '微信',
            align: 'center',
            width: 50,
            readOnly: true,
            menuDisabled: true,
            items: [{
                text: '绑定',
                tooltip: '绑定分机扫码微信',
                iconCls: 'x-fa fa-weixin',
                handler: function (colbtn, rowIndex, cellIndex, item, e, record, row) {
                    var showX = colbtn.getX() + colbtn.up().getWidth(),
                        showY = colbtn.getY() + (rowIndex * 33);
                    var roleguid = record.get('roleid');
                    var extension = record.get('extension');
                    Ext.create("Ext.window.Window", {
                        header: false,
                        listeners: {
                            blur: function () {
                                this.close();
                            },
                            afterrender: function () {
                                var me = this;
                                Ext.Ajax.request({
                                    url: '/tab/rbac/QRCode',
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    params: {
                                        type: 90,
                                        typevalue: roleguid,
                                        data:extension,
                                        width: 172
                                    },
                                    failure: function (response, opts) {
                                        console.log('server-side failure with status code ' + response.status);
                                    },
                                    success: function (response, opts) {
                                        var imgData = Ext.decode(response.responseText);
                                        if (imgData.success) {
                                            me.down('#tooltip').setText('请用微信扫码绑定');
                                            me.down('image').setSrc(imgData.data);
                                        } else {
                                            me.down('image').setSrc("/images/qrcode_timeout.png");
                                            me.down('#tooltip').setText('请用微信扫码');
                                        }
                                    }
                                })
                            }
                        },
                        width: 172,
                        height: 207,
                        x: showX,
                        y: showY,
                        resizable: false,
                        items: [{
                            xtype: 'image',
                            width: 172,
                            height: 172,
                            src:'../images/qrcode_init.gif'
                        }],
                        buttons: [{
                            xtype: 'component',
                            flex: 1
                        },{
                            itemId: 'tooltip',
                            xtype: 'label'
                        },{
                            xtype: 'component',
                            flex: 1
                        }]
                    }).showAt(e.getXY());
                }
            }]
        }*/],
        store: Ext.create('Ext.data.Store', {
            fields: [{
                    name: 'rolename',
                    mapping: 'rolename'
                },
                {
                    name: 'roleid',
                    mapping: 'roleid'
                },
                {
                    name: 'extension',
                    mapping: 'extension'
                }
            ],
            proxy: {
                type: 'rest',
                url: '/tab/rec/UIGetRecExtension',
                actionMethods: {
                    read: 'POST'
                },
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                reader: {
                    type: 'json',
                    rootProperty: 'extensions'
                }
            },
            autoload: true,
        }),
        viewConfig: {
            // columnsText: '显示的列',//显示的列所定义名字
            // scrollOffset: 30,//留出的滚动条宽度
            // sortAscText: '升序',
            loadMask: true,
            enableTextSelection: true
        },
        tbar: {
            items: [{
                    text: '更新',
                    handler: function () {
                        var grid = this.up().up();
                        Ext.create("Ext.window.Window", {
                            title: "更新分机",
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
                                            name: "extensionname",
                                            fieldLabel: "分机号",
                                            labelWidth: 60,
                                            allowBlank: false
                                        },
                                        {
                                            xtype: "container",
                                            layout: "hbox",
                                            items: [{
                                                xtype: "textfield",
                                                width: 260,
                                                emptyText: '开始IP地址',
                                                name: "ipaddressname",
                                                fieldLabel: "IP地址",
                                                labelWidth: 60,
                                                allowBlank: true
                                            },{
                                                xtype: "textfield",
                                                width: 180,
                                                emptyText: '策略编号[0-7]',
                                                name: "policyname",
                                                fieldLabel: "策略",
                                                labelWidth: 60,
                                                allowBlank: true
                                            }
                                            ]
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
                                    var extension = win.down('textfield[name="extensionname"]').getValue();
                                    var ipaddress = win.down('textfield[name="ipaddressname"]').getValue();
                                    var policy = win.down('textfield[name="policyname"]').getValue();
                                    var extensions = extension;
                                    var extensiongroup = new Array();
                                    var groupguid = win.down('combobox[name="groupname"]').getValue();
                                    //判断输入的分机号格式
                                    if (
                                        typeof (extension) != 'undefined' && extension.length > 0 &&
                                        typeof (groupguid) != 'undefined' && groupguid != null) {
                                        var rng = /^[0-9,-]*[0-9]{1,6}$/;
                                        var rrr = /^[0-9]+$/;
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
                                                url: '/tab/rec/UIUpdateRecExtension',
                                                method: 'POST',
                                                headers: {
                                                    'Content-Type': 'application/x-www-form-urlencoded'
                                                },
                                                async: true,
                                                params: {
                                                    serverAddress: grid.down('combobox[name="serverAddress"]').getValue(),
                                                    extensionList: extensiongroup,
                                                    roleguid: groupguid,
                                                    ipaddress: ipaddress,
                                                    policy: policy
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
                }, {
                    text: '删除',
                    handler: function () {
                        var extensionList = new Array();
                        var grid = this.up().up();
                        selectRecords = grid.getSelection();
                        if (Ext.isEmpty(selectRecords)) {
                            Ext.toast({
                                html: '没有选择任何记录',
                                title: '提示',
                                width: 200,
                                align: 'br'
                            });
                            return;
                        } else if (selectRecords.length > 0) {
                            Ext.Array.each(selectRecords, function (record) {
                                var extensionrecord = record.get('extension');
                                extensionList.push(extensionrecord);
                            });
                        };
                        Ext.Msg.confirm('删除', '确定要删除吗?', function (btn) {
                            if (btn == 'yes') {
                                Ext.Ajax.request({
                                    url: '/tab/rec/UIRemoveRecExtension',
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    params: {
                                        serverAddress: grid.down('combobox[name="serverAddress"]').getValue(),
                                        extensionList: extensionList
                                    },
                                    failure: function (response, opts) {
                                        console.log('server-side failure with status code ' + response.status);
                                    },
                                    success: function (response, opts) {
                                        var obj = Ext.decode(response.responseText);
                                        if (obj.success) {
                                            grid.store.load();
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
                    text: '过滤',
                    handler: function () {
                        var grid = this.up().up();
                        Ext.create("Ext.window.Window", {
                            title: "查询分机",
                            width: 300,
                            height: 150,
                            resizable: false,
                            bodyPadding: '5 0 0 5',
                            items: [{
                                xtype: "form",
                                defaults: {},
                                anchor: '100%',
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
                                        width: 275,
                                        emptyText: '输入过滤条件',
                                        name: "extensionname",
                                        fieldLabel: "分机范围",
                                        labelWidth: 60,
                                        allowBlank: true
                                    }]
                                }]
                            }],
                            buttonAlign: 'right',
                            buttons: [{
                                xtype: "button",
                                text: "查询",
                                margin: '0 31 0 0',
                                handler: function () {
                                    var extension = this.up().up().down('textfield[name="extensionname"]').getValue();
                                    if(extension.length>0){
                                        grid.store.filter([{
                                            property: "extension",
                                            value: eval("/" + extension + "/")
                                        }])
                                    }else{
                                        grid.store.clearFilter();
                                    }
                                    this.up("window").close();
                                }


                            }]
                        }).show();
                    }
                },
                {
                    text: '刷新',
                    handler: function () {
                        var grid = this.up().up();
                        grid.getStore().load();
                    }
                },
                {
                    xtype: "combobox",
                    allowBlank: false,
                    name: 'serverAddress',
                    forceSelection: true,
                    emptyText: '选择服务器',
                    width: 200,
                    triggerAction: 'all',
                    valueField: 'serverAddress',
                    displayField: 'serverAddress',
                    editable: false,
                    store: Ext.create('Ext.data.Store', {
                        proxy: {
                            type: 'rest',
                            url: '/tab/rec/UIGetServerList',
                            actionMethods: {
                                read: 'POST'
                            },
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded'
                            },
                            reader: {
                                type: 'json',
                                rootProperty: 'items'
                            }
                        },
                        autoload: true
                    }),
                    listeners:{
                        'change': function (me, newValue, oldValue, eOpts) {
                            var grid = this.up().up();
                            grid.getStore().proxy.extraParams = {
                                serverAddress: newValue
                            }
                            grid.getStore().load();
                        }
                    }
                }
            ]
        },
    }, {
        xtype: 'gridpanel',
        margin: '0 0 0 10',
        title: '修改录音所属部门',
        flex: 1,
        requires: [
            'Ext.window.Toast',
            'Rbac.view.record.SearchRecordStore',
            'Rbac.view.record.SearchRecordModel'
        ],
        autoScroll: true,
        scrollable: true,
        selModel: Ext.create('Ext.selection.CheckboxModel', {
            mode: 'SIMPLE',
            checkOnly: true,
            hidden: true
        }),
        store: Ext.create('Rbac.view.record.SearchRecordStore'),
        dockedItems: [{
            xtype: 'pagingtoolbar',
            dock: 'bottom',
            pageSize: 10,
            displayInfo: true,
            displayMsg: '显示第{0}条到{1}条数据,一共{2}条',
            emptyMsg: '没有记录',
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
                    bgbar.add('条/页');
                    bgbar.setStore(bgbar.up().store);
                }
            }
        }],
        tbar: {
            autoScroll: true,
            items: [{
                    xtype: 'container',
                    layout: 'vbox',
                    items: [{
                            xtype: 'button',
                            text: '查 询',
                            width: 90,
                            listeners: {
                                'click': function () {
                                    var grid = this.up().up().up(),
                                        me = this.up().up();
                                    if (grid.down('datetimetowfield[name="starttime"]').getValue() === null || grid.down('datetimetowfield[name="starttime"]').getValue() === '') {
                                        grid.down('datetimetowfield[name="starttime"]').focus();
                                    }
                                    if (grid.down('datetimetowfield[name="endtime"]').getValue() === null || grid.down('datetimetowfield[name="endtime"]').getValue() === '') {
                                        grid.down('datetimetowfield[name="endtime"]').focus();
                                    }
                                    grid.store.proxy.extraParams = {
                                        starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                                        endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                                        caller: me.down('textfield[name="caller"]').getValue(),
                                        called: me.down('textfield[name="called"]').getValue(),
                                        agent: me.down('combobox[name="agent"]').getValue(),
                                        backup: 0,
                                        lock: 0,
                                        delete: 0,
                                        answer: 0,
                                        direction: 0,
                                        inside: 0,
                                        length: me.down('spinnerfield[name="minseconds"]').getValue(),
                                        extension: me.down('textfield[name="extension"]').getValue(),
                                        maxlength: 0,
                                        guidid: '',
                                        ucid: '',
                                        groupguidid: me.down('combobox[name="group"]').getValue()
                                    };
                                    grid.store.loadPage(1);
                                }
                            }
                        },
                        {
                            xtype: 'container',
                            defaults: {
                                menu: [{
                                    text: '修改全部',
                                    listeners: {
                                        'click': function () {
                                            var me = this.up().up().up().up().up(),
                                                grid = me.up();
                                            Ext.create("Ext.window.Window", {
                                                title: "选择部门",
                                                width: 480,
                                                height: 150,
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
                                                        }]
                                                    }]
                                                }],
                                                buttonAlign: 'right',
                                                buttons: [{
                                                    xtype: "button",
                                                    text: "确定",
                                                    margin: '0 31 0 0',
                                                    handler: function () {
                                                        var win = this.up("window");
                                                        var groupguid = this.up().up().down('combobox[name="groupname"]').getValue();
                                                        if (typeof (groupguid) != 'undefined' && groupguid != null) {
                                                            Ext.Ajax.request({
                                                                url: '/tab/rec/UIUpdateRecfilesRole',
                                                                method: 'POST',
                                                                headers: {
                                                                    'Content-Type': 'application/x-www-form-urlencoded'
                                                                },
                                                                params: {
                                                                    newroleguid: groupguid,
                                                                    starttime: me.down('datetimetowfield[name="starttime"]').getSubmitValue() + ":00",
                                                                    endtime: me.down('datetimetowfield[name="endtime"]').getSubmitValue() + ":59",
                                                                    caller: me.down('textfield[name="caller"]').getValue(),
                                                                    called: me.down('textfield[name="called"]').getValue(),
                                                                    agent: me.down('combobox[name="agent"]').getValue(),
                                                                    backup: 0,
                                                                    lock: 0,
                                                                    delete: 0,
                                                                    answer: 0,
                                                                    direction: 0,
                                                                    inside: 0,
                                                                    length: me.down('spinnerfield[name="minseconds"]').getValue(),
                                                                    extension: me.down('textfield[name="extension"]').getValue(),
                                                                    maxlength: 0,
                                                                    guidid: '',
                                                                    ucid: '',
                                                                    groupguidid: me.down('combobox[name="group"]').getValue()
                                                                },
                                                                failure: function (response, opts) {
                                                                    console.log('server-side failure with status code ' + response.status);
                                                                },
                                                                success: function (response, opts) {
                                                                    var obj = Ext.decode(response.responseText);
                                                                    if (obj.success) {
                                                                        win.close();
                                                                        grid.store.reload();
                                                                    } else {
                                                                        Ext.Msg.alert("错误", obj.msg);
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            Ext.Msg.alert('错误', '部门不能为空');
                                                        }
                                                    }
                                                }]
                                            }).show();
                                        }
                                    }
                                }]
                            },
                            items: [{
                                xtype: 'splitbutton',
                                text: '修 改',
                                width: 90,
                                listeners: {
                                    'click': function (button) {
                                        var grid = this.up().up().up().up(),
                                            selectRecords = grid.getSelection(),
                                            me = this.up().up().up();
                                        if (Ext.isEmpty(selectRecords)) {
                                            Ext.toast({
                                                html: '没有选择任何录音记录',
                                                title: '提示',
                                                width: 200,
                                                align: 'br'
                                            });
                                            return;
                                        }
                                        var guidlist = new Array();
                                        Ext.Array.each(selectRecords, function (record) {
                                            guidlist.push(record.get('sGuid'));
                                        });

                                        Ext.create("Ext.window.Window", {
                                            title: "选择部门",
                                            width: 480,
                                            height: 150,
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
                                                    }, ]
                                                }]
                                            }],
                                            buttonAlign: 'right',
                                            buttons: [{
                                                xtype: "button",
                                                text: "确定",
                                                margin: '0 31 0 0',
                                                handler: function () {
                                                    var win = this.up("window");
                                                    var groupguid = this.up().up().down('combobox[name="groupname"]').getValue();
                                                    //判断输入的分机号格式
                                                    if (
                                                        typeof (guidlist) != 'undefined' && guidlist.length > 0 &&
                                                        typeof (groupguid) != 'undefined' && groupguid != null) {
                                                        Ext.Ajax.request({
                                                            url: '/tab/rec/UIUpdateRecfiles',
                                                            method: 'POST',
                                                            headers: {
                                                                'Content-Type': 'application/x-www-form-urlencoded'
                                                            },
                                                            async: true,
                                                            params: {
                                                                idList: guidlist,
                                                                roleguid: groupguid
                                                            },
                                                            failure: function (response, opts) {
                                                                console.log('server-side failure with status code ' + response.status);
                                                            },
                                                            success: function (response, opts) {
                                                                var obj = Ext.decode(response.responseText);
                                                                if (obj.success) {
                                                                    win.close();
                                                                    grid.store.reload();
                                                                } else {
                                                                    Ext.Msg.alert('错误', '请重新选择');
                                                                }
                                                            }
                                                        })
                                                    } else {
                                                        Ext.Msg.alert('错误', '部门不能为空');
                                                    }
                                                }
                                            }]
                                        }).show();
                                    }
                                }
                            }]
                        }
                    ]
                }, {
                    xtype: 'container',
                    items: [{
                            xtype: 'datetimetowfield',
                            width: 280,
                            fieldLabel: '开始时间',
                            name: 'starttime',
                            labelWidth: 60,
                            timeCfg: {
                                value: '00:00'
                            }
                        },
                        {
                            xtype: 'datetimetowfield',
                            width: 280,
                            fieldLabel: '结束时间',
                            name: 'endtime',
                            labelWidth: 60,
                            timeCfg: {
                                value: '23:59'
                            }
                        }
                    ]
                },
                {
                    xtype: 'container',
                    items: [{
                        xtype: 'combobox',
                        width: 180,
                        fieldLabel: '组名',
                        name: 'group',
                        emptyText: '全部',
                        labelWidth: 30,
                        forceSelection: true,
                        editable: true,
                        queryDelay: 60000,
                        triggerAction: 'all',
                        valueField: 'roleguid',
                        displayField: 'rolename',
                        store: Ext.create('Rbac.view.record.groupsMenuStore'),
                        listeners: {
                            'select': function (me, record) {
                                var roleguid = record.get('roleguid');
                                var combo = me.up().down('combobox[name="agent"]');
                                combo.setValue('');
                                combo.store.proxy.extraParams = {
                                    roleguid: roleguid
                                };
                                combo.store.reload();
                            }
                        }
                    }, {
                        xtype: 'combobox',
                        width: 180,
                        fieldLabel: '座席',
                        name: 'agent',
                        emptyText: '全部',
                        labelWidth: 30,
                        editable: true,
                        queryDelay: 60000,
                        triggerAction: 'all',
                        displayField: 'nickname',
                        valueField: 'userguid',
                        store: Ext.create('Rbac.view.record.usersMenuStore')
                    }]
                },
                {
                    xtype: 'container',
                    items: [{
                            xtype: 'textfield',
                            width: 180,
                            fieldLabel: '主叫号码',
                            name: 'caller',
                            labelWidth: 65
                        },
                        {
                            xtype: 'textfield',
                            width: 180,
                            fieldLabel: '被叫号码',
                            name: 'called',
                            labelWidth: 65
                        }
                    ]
                },
                {
                    xtype: 'container',
                    items: [{
                            xtype: 'spinnerfield',
                            width: 150,
                            fieldLabel: '最小时长(秒)',
                            name: 'minseconds',
                            labelWidth: 80,
                            value: 0,
                            editable: true,
                            allowBlank: false,
                            onSpinUp: function () {
                                var me = this;
                                if (!me.readOnly) {
                                    var val = parseInt(me.getValue(), 10) || 0;
                                    me.setValue(val + 1);
                                }
                            },
                            onSpinDown: function () {
                                var me = this;
                                if (!me.readOnly) {
                                    var val = parseInt(me.getValue(), 10) || 0;
                                    if (val > 0) {
                                        me.setValue(val - 1);
                                    }
                                }
                            }
                        },
                        {
                            xtype: 'textfield',
                            width: 150,
                            fieldLabel: '分机号码',
                            name: 'extension',
                            labelWidth: 60
                        }
                    ]
                }
            ]
        },
        columns: [{
                header: "分机",
                align: 'center',
                width: 100,
                dataIndex: 'sExtension',
                menuDisabled: true,
                filter: {
                    type: 'string',
                    emptyText: '请输入分机号'
                }
            },
            {
                header: "工号",
                align: 'center',
                width: 100,
                dataIndex: 'sAgent',
                menuDisabled: true,
                filter: {
                    type: 'string',
                    emptyText: '请输入工号'
                }
            },
            {
                header: "姓名",
                align: 'center',
                menuDisabled: true,
                width: 100,
                dataIndex: 'sUserName'
            },
            {
                header: "时间",
                align: 'center',
                menuDisabled: true,
                width: 160,
                dataIndex: 'sSystemTim',
                filter: {
                    emptyText: '请输入日期'
                }
            },
            {
                header: "时长",
                width: 100,
                align: 'center',
                dataIndex: 'nSeconds',
                menuDisabled: true,
                renderer: function (d) {
                    var hour = Math.floor(d / (3600)); //小时
                    var h = d % (3600);
                    var minute = Math.floor(h / (60)); //分钟
                    var m = h % (60);
                    var second = Math.floor(m);
                    var hh = (hour < 10 ? "0" + hour : hour)
                    var mm = (minute < 10 ? "0" + minute : minute)
                    var ss = (second < 10 ? "0" + second : second)
                    return hh + ":" + mm + ":" + ss;
                },
                filter: {
                    type: 'number',
                    emptyText: '请输入时长'
                }
            },
            {
                header: "方向",
                width: 80,
                align: 'center',
                menuDisabled: true,
                dataIndex: 'nDirection',
                renderer: function (v, m) {
                    if (v == 0 || v == 2) {
                        m.tdCls = 'inbound-cell';
                        return "呼入";
                    } else if (v == 1 || v == 3) {
                        m.tdCls = 'outbound-cell';
                        return "呼出";
                    }
                    return "其他";
                }
            },
            {
                header: "主叫",
                width: 100,
                menuDisabled: true,
                align: 'center',
                dataIndex: 'sCaller',
                filter: {
                    type: 'string',
                    emptyText: '请输入主叫'
                }
            },
            {
                header: "被叫",
                width: 100,
                menuDisabled: true,
                align: 'center',
                dataIndex: 'sCalled',
                filter: {
                    type: 'string',
                    emptyText: '请输入被叫'
                }
            },
            {
                header: "分机组",
                width: 100,
                menuDisabled: true,
                align: 'center',
                dataIndex: 'sGroupName',
                filter: {
                    type: 'string',
                    emptyText: '请输入分机组'
                }
            }
        ],
        viewConfig: {
            loadMask: true,
            enableTextSelection: true
        }
    }]
});