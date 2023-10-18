Ext.define('Tab.view.main.MainModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.main',

    data: {
        userName: '请登录...',
        currentView: null
    }
});

function standartLength(d){
	var hour = Math.floor(d/(3600));//小时
	var h = d%(3600);
	var minute=Math.floor(h/(60)) ;//分钟
	var m = h%(60); 
	var second=Math.floor(m) ;
	var hh = (hour < 10 ? "0" + hour : hour)
	var mm = (minute < 10 ? "0" + minute : minute)
	var ss = (second < 10 ? "0" + second : second)
	return hh+":"+mm+":"+ss;
}