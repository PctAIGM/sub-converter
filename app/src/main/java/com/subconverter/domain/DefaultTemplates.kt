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
