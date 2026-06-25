package com.subconverter.domain

const val DEFAULT_MIHOMO_TEMPLATE = """
mixed-port: 7890
allow-lan: false
mode: rule
log-level: info
proxy-groups:
  - name: PROXY
    type: select
    proxies: "{{proxy_names}}"
rules:
  - MATCH,PROXY
"""

const val DEFAULT_OVERRIDE_YAML = """
rules+: []
"""

const val DEFAULT_OVERRIDE_JS = """
function main(config) {
  // config 为解析后的完整配置对象，返回修改后的对象即可
  // 在 rules 开头插入一条规则：
  config.rules.unshift("DOMAIN,google.com,DIRECT");
  return config;
}
"""
