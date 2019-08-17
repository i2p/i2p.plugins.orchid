setInterval(function() {
	var xhr = new XMLHttpRequest();
	xhr.open("GET", "/orchid/?" + new Date().getTime(), true);
	xhr.responseType = "text";
	xhr.onreadystatechange = function () {
		if (xhr.readyState==4 && xhr.status==200) {
			document.getElementById("orchid").innerHTML = xhr.responseText;
		}
	}
	xhr.send();
}, 15000);