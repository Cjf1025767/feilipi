var Exports = Object();
Exports.Name = function(){return "录音查询结果";}
Exports.Header = function(){return [
["backup", "备份状态"],
["delete", "删除状态"],
["lock", "加锁状态"],
["direction", "呼叫状态"],
["host", "录音主机"],
["seconds", "时长(秒)"],
["agent", "工号"],
["called", "被叫"],
["caller", "主叫"],
["extension", "分机"],
["role.rolename", "所属组"],
["id", "录音标识"],
["createdate", "开始时间"],
["ucid", "数据"],
["username", "姓名"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		default:
			lst[i] = '\t' + records.get(i);
			break;
		}
	}
	return lst;
}
