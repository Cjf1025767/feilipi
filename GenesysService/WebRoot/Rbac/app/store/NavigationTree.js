Ext.define('Rbac.store.NavigationTree', {
    extend: 'Ext.data.TreeStore',

    storeId: 'NavigationTree',

    fields: [{
        name: 'text'
    }],

    root: {
        expanded: true,
        children: [
			{
				text:'欢迎',
				iconCls: 'x-fa fa-home',
				leaf:true,
				routeId: 'dashboard',
				viewType: 'dashboardView'
			},{
				resourceId: "45BBBF87-25B8-492A-B206-CC55F3E4903C",
				text:'录音查询',
				iconCls: 'x-fa fa-file-audio-o',
				leaf:true,
				routeId: 'searchrecord',
				viewType: 'searchrecordview'
			},{
				resourceId: "45BBBF87-25B8-492A-B206-CC55F3E4903C",
				text:'录音汇总',
				iconCls: 'x-fa fa-bar-chart',
				leaf:true,
				routeId: 'recordsummaryview',
				viewType: 'recordsummaryview'
			},
			{
				resourceId: "1A16A347-A2CF-11E9-9604-54E1AD6C1F93",
				text:'录音监控',
				iconCls: 'x-fa fa-line-chart',
				leaf:true,
				routeId: 'recordmonitorview',
				viewType: 'recordmonitorview'
			},
			{
                text: '权限管理',
				iconCls: 'x-fa fa-table',
				expanded: true,
                selectable: false,
				children:[
					{
						resourceId: "6B46C170-833F-413C-9D90-D887817B3E34",
						text: '分机设置',
						iconCls:'x-fa fa-cog',
						leaf:true,
						routeId: 'settingextension',
						viewType: 'settingextensionview'
					},
					{
						resourceId: "6B46C170-833F-413C-9D90-D887817B3E34",
						text: '部门参数',
						iconCls:'x-fa fa-address-card-o',
						leaf:true,
						routeId: 'settingskill',
						viewType: 'settingskillview'
					},
					{
						resourceId: "6B46C170-833F-413C-9D90-D887817B3E34",
						text: '分组设置',
						iconCls: 'x-fa fa-users',
						leaf:true,
						routeId: 'groupinfo',
						viewType: 'groupInfoSetting'
					},
					{
						resourceId: "6B46C170-833F-413C-9D90-D887817B3E34",
						text: '用户设置',
						iconCls: 'x-fa fa-user',
						leaf:true,
						routeId: 'userinfo',
						viewType: 'UserInfoSetting'
					}
				]
			}
        ]
    }
});
