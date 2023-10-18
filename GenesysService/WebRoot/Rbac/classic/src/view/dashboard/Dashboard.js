Ext.define('Rbac.view.dashboard.Dashboard', {
	extend: 'Ext.container.Container',
	xtype: 'dashboardView',

	requires: [
		'Ext.panel.Panel',
		'Ext.plugin.Responsive',
		'Ext.button.Button',
		'Ext.layout.container.Accordion'
	],

	layout: {
		type: 'hbox',
		align: 'stretch'
	},
	padding: 10,
	items: [{
		xtype: 'panel',
		ui: 'light',
		margin: 10,
		flex: 1,
		cls: 'pages-faq-container shadow',
		bodyPadding: 15,
		iconCls: 'x-fa fa-info',
		title: "Welcome, Let's grow up together.",

		items: [
			{
				xtype: 'panel',
				cls: 'FAQPanel',
				title: '权限管理',
				iconCls: 'x-fa fa-key',
				bodyPadding: 10,
				ui: 'light',
				items: [{
						title: '分机设置(仅录音系统)',
						cls: 'dashboardTitle',
						iconCls: 'x-fa fa-caret-down',
						html: '1) 配置分机所属部门<br />' +
							'2) 根据分机所属部门，控制查询权限<br />' +
							'3) 通用权限包含:查询权限，播放权限，导出权限'
					},
					{
						title: '分组设置',
						cls: 'dashboardTitle',
						iconCls: 'x-fa fa-caret-down',
						html: '1) 支持多级分组设置<br />' +
							'2) 可以为不同的组分配不同权限<br />' +
							'3) 组员仅可查询子组录音<br />'
					},
					{
						title: '用户设置',
						cls: 'dashboardTitle',
						iconCls: 'x-fa fa-caret-down',
						html: '1) 增删登录用户<br />' +
							'2) 每个用户可以属于多个组<br />' +
							'2) 用户获得所属组的权限,如果属于多个组,则获取多个组的权限<br />'
					}
				]
			},{
				xtype: 'panel',
				iconCls: 'x-fa fa-file-audio-o',
				title: '录音查询(仅录音系统)',
				bodyPadding: 10,
				ui: 'light',
				items: [{
					title: '模块',
					cls: 'dashboardTitle',
					iconCls: 'x-fa fa-caret-down',
					html: '1) 支持多条件查询, 主被叫号码模糊匹配<br />' +
						'2) 支持多种通用浏览器,支持播放和批量下载录音<br />' +
						'3) 多录音主机统一查询<br />' +
						'4) 语音转文字,播放语音同步显示文字内容,并可选择文字播放对应的语音(需要语音识别模块,以及非IE浏览器)'
				}]
			},
			{
				xtype: 'panel',
				cls: 'FAQPanel',
				title: 'FAQ',
				iconCls: 'x-fa fa-question-circle',
				bodyPadding: 10,
				ui: 'light',
				items: [{
					title: 'UCID是什么意思?',
					cls: 'dashboardTitle',
					iconCls: 'x-fa fa-caret-down',
					html: '全称Universal Call ID,配合呼叫中心CTI中间件时才有用处,表示呼叫的唯一标识'
				}]
			},
			{
				html:'<img src="/images/qrcodeImage.png" height="172" width="172" />'
			}
		]
	}]
});