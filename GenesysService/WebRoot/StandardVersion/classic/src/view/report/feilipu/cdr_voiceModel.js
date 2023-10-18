

Ext.define('Tab.view.report.feilipu.cdr_voiceModel',{
	extend: 'Ext.data.Model',
	 fields: [
			{name: "ani"},
			{name: "dnis"},
            {name: "vdn"},
            {name: "queuetime"},
			{name: "starttime",convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
			{name: "endtime",convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
            {name: "type"},
            {name: "talklength"},
            {name: "agent"},
            {name: "agent_last_name"},
            {name: "department"}
	      ]
});
Ext.define('Tab.view.report.feilipu.cdr_voiceStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.feilipu.cdr_voiceModel',
    autoLoad: false,
    // remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/FLPrec/UISearchCdrVoice',
		timeout: 120000,
        actionMethods : {read: 'POST'},
		headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        reader : {
            type : 'json',
            rootProperty : 'list',
            totalProperty : 'totalCount'
        }
    }
});