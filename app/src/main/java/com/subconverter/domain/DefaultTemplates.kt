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

const val DEFAULT_OVERRIDE_RULES = ""

const val BUILTIN_PROXY_GROUPS_OVERRIDE_NAME = "代理组（内置）"

const val BUILTIN_PROXY_GROUPS_OVERRIDE_YAML = """
proxy-groups!:
  - name: 节点选择
    type: select
    proxies:
      - 全部节点
      - 自动选择
      - 港澳
      - 台湾
      - 日本
      - 韩国
      - 新加坡
      - 东南亚
      - 美国
      - 其它地区
      - DIRECT
  - name: 广告拦截
    type: select
    proxies:
      - REJECT
      - DIRECT
      - 节点选择
  - name: 港澳
    type: select
    include-all: true
    filter: "(?i)港|hk|hongkong|hong kong|澳门|mo|macao|macau"
  - name: 台湾
    type: select
    include-all: true
    filter: "(?i)台|tw|taiwan"
  - name: 日本
    type: select
    include-all: true
    filter: "(?i)日本|jp|japan"
  - name: 韩国
    type: select
    include-all: true
    filter: "(?i)韩|korea|kr|韓國|韩国"
  - name: 美国
    type: select
    include-all: true
    filter: "(?i)美|us|unitedstates|united states"
  - name: 新加坡
    type: select
    include-all: true
    filter: "(?i)(新|sg|singapore)"
  - name: 东南亚
    type: select
    include-all: true
    filter: "(?i)马来|馬來|my|malaysia|泰国|泰國|th|thailand|越南|vn|vietnam"
  - name: 其它地区
    type: select
    include-all: true
    filter: "(?i)^(?!.*(?:🇭🇰|🇲🇴|🇯🇵|🇰🇷|🇺🇸|🇸🇬|🇲🇾|🇹🇭|🇻🇳|🇨🇳|港|hk|hongkong|澳门|mo|macao|macau|台|tw|taiwan|日|jp|japan|韩|korea|kr|新|sg|singapore|马来|馬來|my|malaysia|泰国|泰國|th|thailand|越南|vn|vietnam|美|us|unitedstates)).*"
  - name: 全部节点
    type: select
    include-all: true
  - name: 自动选择
    type: url-test
    include-all: true
    url: https://cp.cloudflare.com
    interval: 300
    tolerance: 100

rules!:
  - GEOSITE,CN,DIRECT
  - GEOIP,CN,DIRECT
  - MATCH,节点选择
"""
