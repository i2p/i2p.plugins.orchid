var expandConfig = document.getElementById("expandConfig");
var collapseConfig = document.getElementById("collapseConfig");
var config = document.getElementById("configuration");

function hideConfig() {
  if (!collapseConfig)
    config.style.display = "none";
}

function showConfig() {
  if (collapseConfig)
    config.style.display = "block";
}

function clean() {
  if (expandConfig) {
      expandConfig.remove();
  }
  if (collapseConfig) {
      collapseConfig.remove();
  }
}

function expand() {
  clean();
  var x = document.createElement("link");
  x.type="text/css";
  x.rel="stylesheet";
  x.href="/orchid/resources/expand.css";
  x.setAttribute("id", "expandConfig");
  document.head.appendChild(x);
  showConfig();
}

function collapse() {
  clean();
  var c = document.createElement("link");
  c.type="text/css";
  c.rel="stylesheet";
  c.href="/orchid/resources/collapse.css";
  c.setAttribute("id", "collapseConfig");
  document.head.appendChild(c);
  hideConfig();
}

function copyText() {
  document.execCommand("copy");
}
