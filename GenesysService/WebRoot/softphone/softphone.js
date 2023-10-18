

var consultFlag = 0;
var transferFlag = 0;//转接或咨询不需要弹屏
var conferenceFlag = 0;
var IsLogged = false;

var agent=""
var extension=""
readystatus=false

window.onload=function(){
		
}

function SetBgImg(vstr){
	document.getElementById("statusLED").innerHTML = vstr;
} 

// 座席登录
function AgentLogin(){
	

	var AgentDN = prompt("请输入你的分机号","1002"); 
	
	if (MyPhoner.Extension != AgentDN){
		MyPhoner.Login(AgentDN, '1', AgentDN, '', '', MyPhonerEvent);
	}
}

//注销
function AgentLogOut(){
	
	IsLogged = false;
    MyPhoner.Logout();
}
// 就绪
function UserReady(){
	readystatus=true
    MyPhoner.Ready();
}
// 休息
function UserNotReady(state){
	if (typeof(state) == "undefined") {
		state = 0;
	}
	
    MyPhoner.NotReady(state);
}
//呼出

function UserDial(){
	
	var phonenumber = prompt("请输入对方号码",""); 
	var data={};
	  MyPhoner.Dial(phonenumber,data);
}
// 接听电话
function UserAnswerCall(){
    MyPhoner.Answer();
}
// 挂电话
function UserHangupCall(){
  

	MyPhoner.Hangup();


}
// 保持
function UserHold(){
	return MyPhoner.Hold();
}
// 接回
function UserReconnect(){
	  return MyPhoner.Unhold();
}
// 咨询






function clearEnent(){//移除所有事件
	var eventTable = document.getElementById("eventlist");
		
     var rowNum=eventTable.rows.length;
     for (i=1;i<rowNum;i++)
     {
         eventTable.deleteRow(i);
         rowNum=rowNum-1;
         i=i-1;
     }
	
}
function MyPhonerEvent(sMessage){//事件信息
         ButtonStatus(sMessage);
	    var eventTable = document.getElementById("eventlist");
	
		var newTr = eventTable.insertRow(-1);
		var newTd0 = newTr.insertCell(-1);
		newTd0.innerHTML = eventTable.rows.length - 1;
		var newTd1 = newTr.insertCell(-1);
		newTd1.innerHTML = agent;
		var newTd2 = newTr.insertCell(-1);
		newTd2.innerHTML = sMessage;
		var newTd3 = newTr.insertCell(-1);
		newTd3.innerHTML = getCurrentTimeforNow();
        
}


    var getCurrentTimeforNow = function() {
        var date = new Date() //当前时间
        date = date.getTime()
        date = new Date(date)
        var month = (date.getMonth() + 1).toString().padStart(2, '0') //月
        var day = date
            .getDate()
            .toString()
            .padStart(2, '0') //日
        var hour = date
            .getHours()
            .toString()
            .padStart(2, '0') //时
        var minute = date
            .getMinutes()
            .toString()
            .padStart(2, '0') //分
        var second = date
            .getSeconds()
            .toString()
            .padStart(2, '0') //秒
            //当前时间
        var curTime =
            date.getFullYear() +
            '-' +
            month +
            '-' +
            day +
            ' ' +
            hour +
            ':' +
            minute +
            ':' +
            second
        return curTime
    }
function ButtonStatus(EventStr) {//获取当前状态
	
		
	
	if (EventStr.indexOf('Login')>=0 && IsLogged==false ) {// 登录
		IsLogged = true;

	
		agent=MyPhoner.GetLoginInfo().agent
		extension=MyPhoner.GetLoginInfo().extension
		document.getElementById("agentID").innerHTML = agent;
		document.getElementById("agentDN").innerHTML = extension;
	
	
		
	}
	if ((EventStr.indexOf('Logout')>=0 || EventStr.indexOf('Logout')>=0) ) {// 退出
		IsLogged = false;
		SetBgImg('未登录');
		
		
		agent=""
		extension=""
		document.getElementById("agentID").innerHTML = agent;
		document.getElementById("agentDN").innerHTML = extension;
	}
	if (EventStr == 'Ready') {// 示闲
		SetBgImg('就绪');	
		
	}
	if (EventStr == 'NotReady') {// 示忙
	  SetBgImg('未就绪');
		
		
	}
	if (EventStr.indexOf('Ring')>=0) {// 来电（未就绪的振铃可否加个状态？）
			
		SetBgImg('振铃');
	
		
	
	}
	if (EventStr.indexOf('Answer')>=0) {// 通话

		consultFlag = 0;
		transferFlag = 0;
		conferenceFlag = 0;		
		// 计时器
		
		SetBgImg('通话');
	
		
	}
	if (EventStr.indexOf('Connected')>=0) {	
	
		
		SetBgImg('接听');
	
		
		
	}
	if (EventStr.indexOf('Dial')>=0) {
		
	   SetBgImg('呼出');
	
	}
	if (EventStr.indexOf('Disconnect')>=0) {// 挂机
	
	    SetBgImg('挂机');
		
		

	}
	if (EventStr == 'Hold') {// 保持
		SetBgImg('保持');
		
	
	}
	if (EventStr == 'Unhold') {// 取回
		SetBgImg('通话');
		
	
	}
	
}

