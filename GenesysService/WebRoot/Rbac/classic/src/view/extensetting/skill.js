Ext.define('Tab.view.extensetting.skill', {
    extend: 'Ext.panel.Panel',
    xtype: 'settingskillview',
    requires: [
        'Tab.view.extensetting.skillController',
        'Tab.view.extensetting.skillModel',
        'Ext.Img',
        'Ext.window.Window',
        'Ext.Component'
    ],

    controller: 'extensetting-skill',
    viewModel: {
        type: 'extensetting-skill'
    },
    layout: {
        type: 'hbox',
        align: 'stretch'
    },
    items: [{
        width: 500,
        xtype: 'treepanel',
        title: '部门列表',
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
                    Ext.Ajax.request({
                        url: '/tab/rbac/UIGetAppInfo',
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        params: {
                            roleguid: record.get('id')
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
                            var rolepanel = me.up().up().down('#centerId');
                            var form = rolepanel.getForm();
                            form.setValues(obj);
                            form.setValues({
                                userguid: record.get('userguid')
                            });
                            rolepanel.down("textfield[name=createdate]").setValue(Ext.Date.format(new Date(obj.createdate), 'Y-m-d H:i:s'));
                            rolepanel.down("textfield[name=updatedate]").setValue(Ext.Date.format(new Date(obj.updatedate), 'Y-m-d H:i:s'));
                        }
                    });
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
            items: [
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
        }]
    },{
        xtype: 'form',
        itemId: 'centerId',
        width: 360,
        title: '部门参数',
        margin: '0 0 0 10',
        items: [{
            xtype: 'fieldcontainer',
            padding: 5,
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
                        fieldLabel: "URI标识"
                    },
                    {
                        xtype: "textfield",
                        name: "secret",
                        width: 350,
                        labelWidth: 70,
                        fieldLabel: "安全码",
                        margin: 0,
                        readOnly: true
                    },{
                        xtype: "textfield",
                        name: "third_appid",
                        width: 350,
                        labelWidth: 70,
                        fieldLabel: "第三方APPID"
                    },
                    {
                        xtype: "textfield",
                        name: "imserver",
                        width: 350,
                        labelWidth: 70,
                        fieldLabel: "消息服务器(Odoo)"
                    },
                    {
                        xtype: "numberfield",
                        name: "channel",
                        //hideTrigger:true,//隐藏微调按钮
                        allowDecimals:false,//不允许输入小数
                        nanText:'请输入有效的整数',//无效数字提示
                        width: 350,
                        labelWidth: 70,
                        fieldLabel: "会话编号"
                    },
                    {
                        xtype: "textfield",
                        name: "options",
                        width: 350,
                        labelWidth: 70,
                        fieldLabel: "消息参数"
                    },
                    {
                        xtype: "textfield",
                        name: "skillname",
                        width: 350,
                        labelWidth: 70,
                        fieldLabel: "技能组名"
                    },
                    {
                        xtype: "fieldcontainer",
                        layout: "hbox",
                        items:[{
                            xtype: "button",
                            text: "保存",
                            margin: "10 10 10 10",
                            handler: function () {
                                var me = this;
                                var treepanel = me.up('settingskillview').down('treepanel');
                                Ext.Ajax.request({
                                    url: '/tab/rbac/UIUpdateAppInfo',
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    params: {
                                        roleguid: treepanel.store.proxy.extraParams.roleguid,
                                        appid: me.up().up().down("textfield[name=third_appid]").getValue(),
                                        imserver: me.up().up().down("textfield[name=imserver]").getValue(),
                                        options: me.up().up().down("textfield[name=options]").getValue(),
                                        skillname: me.up().up().down("textfield[name=skillname]").getValue(),
                                        channel: me.up().up().down("textfield[name=channel]").getValue(),
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
                            }
                        },{
                            xtype: "button",
                            text: "更新安全码",
                            margin: "10 10 10 10",
                            handler: function () {
                                var me = this;
                                var treepanel = me.up('settingskillview').down('treepanel');
                                Ext.Msg.show({
                                    title: '注意',
                                    message: '更新安全码(Secret)后, 所有APP调用都需要使用新的安全码(Secret)<br />是否更新?',
                                    buttons: Ext.Msg.YESNO,
                                    icon: Ext.Msg.QUESTION,
                                    fn: function (btn) {
                                        if (btn === 'yes') {
                                            Ext.Ajax.request({
                                                url: '/tab/rbac/UIUpdateAppSecretInfo',
                                                method: 'POST',
                                                headers: {
                                                    'Content-Type': 'application/x-www-form-urlencoded'
                                                },
                                                params: {
                                                    roleguid: treepanel.store.proxy.extraParams.roleguid,
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
                        }]
                    }
                ]
            }]
        }]
    },{
		xtype: 'panel',
		ui: 'light',
		margin: 10,
		flex: 1,
		cls: 'pages-faq-container shadow',
		bodyPadding: 15,
		iconCls: 'x-fa fa-info',
		title: "部门参数",
		items: [
			{
				xtype: 'panel',
				cls: 'FAQPanel',
				title: '第三方鉴权支持',
				iconCls: 'x-fa fa-key',
				bodyPadding: 10,
				ui: 'light',
				items: [{
						title: '参数列表',
						cls: 'dashboardTitle',
						iconCls: 'x-fa fa-caret-down',
						html: '1) APP标识: 代表该部门对应的权限验证APPID<br />' +
                            '2) 点击更新安全码按键,生成新的安全码<br />' +
							'3) 第三方模块通过APP标识和安全码, 调用OAuth2接口, 对接该权限模块<br />' +
							'4) 权限模块提供用户列表, 部门列表, 工号列表, 权限列表等功能辅助<br />' +
                            '5) URI标识: 增强的安全参数, 表示允许连接的第三方模块URI, 空不限制连接<br />'
					}
				]
			},{
				xtype: 'panel',
				iconCls: 'x-fa fa-file-audio-o',
				title: '在线客服参数',
				bodyPadding: 10,
				ui: 'light',
				items: [{
					title: '参数列表',
					cls: 'dashboardTitle',
					iconCls: 'x-fa fa-caret-down',
					html: '1) 第三方APPID: 微信公众号ID, 例如小程序:/pages/index/index?appid=公众号ID<br />' +
							'2) 消息服务器: 接收消息的座席服务器URL地址(Odoo网址)<br />' +
							'3) 会话编号: 接收消息的通道ID,大于0的数字, 总部门组的名字为小程序分享的标题,编号必须等于0<br />' +
                            '4) 消息参数：客户端显示参数(Odoo扩展模块自动更新)<br />'
				}]
			},{
				xtype: 'panel',
				iconCls: 'x-fa fa-file-audio-o',
				title: '通信录音(短信)存证带质检接口规范',
				bodyPadding: 10,
				ui: 'light',
				items: [{
					title: '参数列表',
					cls: 'dashboardTitle',
					iconCls: 'x-fa fa-caret-down',
					html: '1) 会话编号: 对应通讯类型(语音/短信),1为语音,2为短信,3为其他<br />' +
                            '2) 消息参数：对应授权业务类型, 还款提醒 03002<br />' +
                            '3) 技能组名：对应企业编号(代码)<br />'

				}]
			},
			{
				xtype: 'panel',
				cls: 'FAQPanel',
				title: 'CTI参数',
				iconCls: 'x-fa fa-question-circle',
				bodyPadding: 10,
				ui: 'light',
				items: [{
					title: '参数列表',
					cls: 'dashboardTitle',
					iconCls: 'x-fa fa-caret-down',
					html: '1) 技能组名: 该部门对应的技能组名称<br />'
				}]
			},
			{
				html:'<img src="/images/qrcodeImage.png" height="172" width="172" />'
			}
		]
	}]
});