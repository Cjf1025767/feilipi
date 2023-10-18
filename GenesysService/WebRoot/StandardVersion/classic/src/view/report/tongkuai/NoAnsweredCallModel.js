Ext.define('Tab.view.report.tongkuai.NoAnsweredCallViewModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.report-NoAnsweredCall',
    data: {
        name: 'WebServer'
    }

});


Ext.define('Tab.view.report.tongkuai.NoAnsweredCallModel',{
	extend: 'Ext.data.Model',
	
	 fields: [
			{name: "callivrrecordId"},
			{name: "callrefid"},
			{name: "ipaddress"},
			{name: "channel"},
			{name: "ani"},
			{name: "dnis"},
			{name: "lastnode",convert:function(v,r){
				if(v!=0){
					return v;
				}else{
					return "";
				}
            }},
			{name: "begintime",convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
			{name: "endtime",convert:function(v,r){
                return Ext.Date.format(new Date(v),'Y-m-d H:i:s');
            }},
            {name: "type"},
            {name: "talklength"},
            {name: "agent"},
            {name: "transfer"},
            {name: "ucid"},
            {name: "host"},
            {name: "vdn"},
            {name: "username"},
            {name: "department"}
	      ]
});
Ext.define('Tab.view.report.tongkuai.NoAnsweredCallStore', {
    extend: 'Ext.data.Store',
    model: 'Tab.view.report.tongkuai.NoAnsweredCallModel',
    autoLoad: false,
    remoteSort: true,
    proxy : {
        type : 'rest',
		url: '/tab/call/NoAnswerCall',
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