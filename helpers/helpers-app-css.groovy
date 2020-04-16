/**
 * APP CSS METHODS (helpers-app-css)
 *
 * Helper functions for App CSS
 */

String getCSSStyles(btnDone=false) {
    String css = '''<style>
/* General App Styles */
#version {
    font-size: 50%;
}
.btn {
    font-family: "Roboto","Helvetica","Arial",sans-serif;
}
/*#formApp h3:first-child {
    display: none;
}*/
.mdl-card, .mdl-switch__label, .mdl-textfield  {
    font-size: 14px;
    font-family: "Roboto","Helvetica","Arial",sans-serif;
}
.btn-sub {
    padding: 2px 30px 2px 2px;
}
div.mdl-button--raised {
    font-weight: bold; 
    color:#fff; 
    background-color:#81bc00; 
    border: 1px solid;
}
div.mdl-button--raised:hover, div.mdl-button--raised:focus {
    color: #212121;
    background-color:#91d844; 
}
.btn-sub.hrefElem:before {
    top: calc(50% - 0.75625rem);
}
div.mdl-button--raised h4.pre {
    font-weight: bold; 
    color: #fff;
    vertical-align: middle;
}

/* Icon Styles */
.icon-position {
    margin-right: 12px;
    vertical-align: middle;
}
.icon-tiny {
    margin-right: 8px;
    font-size: 14px;
}
.icon-small {
    margin-right: 8px;
    font-size: 18px;
}
.icon-large {
    margin-right: 12px;
    font-size: 32px;
}

/* Configure Devices List Styles */
#collapse4 .hrefElem::before {
    filter: invert(100%);
}
#collapse4 .hrefElem:hover::before, #collapse4 .hrefElem:focus::before {
    filter: invert(0%);
}
#collapse4 table .hrefElem::before {
    filter: invert(0%);
}
#collapse4 .btn-block {
    color: #f5f5f5;
    background-color: #382e2b;
    
    font-size: 14px;
    /*font-size: calc(100% + 0.08vw);*/
    max-width: inherit;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
#collapse4 .btn-block span {
    white-space: nowrap !important;
    max-width: inherit;
}
#collapse4 .btn-block:hover, #collapse4 .btn-block:focus {
    color: #212121;
    background-color: #e0e0e0;
}
#collapse4 div.mdl-textfield {
    margin: 0px;
}
.device-config_table {
    border-spacing: 2px 0px;
    table-layout:fixed;
    width: 100%
}
.device-config_td {
    text-align: center;
    vertical-align: middle;
}
.device-config_btn {
    width: 100%;
}
.device-config_table th, .device-config_table td {
    font-family: "Roboto","Helvetica","Arial",sans-serif;
    font-size: 13px;
    vertical-align: middle;
    width: 100%;
}
.device-config_table th div, .device-config_td div, .device-config_table td a {
    text-align: center;
    white-space: nowrap !important;
    max-width: inherit;
    overflow: hidden;
    text-overflow: ellipsis;
    width: 100%;
    display: block;
}
.device-config_btn_icon {
    text-align: center;
    width: 100%;
}

/* Action Buttons */
#collapse4 [name*="refreshDevices"] {
    float: right !important;
}
#collapse4 [name*="resultPage"] {
    float: left !important;
    visibility: hidden;
}
#collapse4 [name*="refreshDevices"], #collapse4 [name*="resultPage"] {
    color: #000;
    width: 170px !important;
    min-width: 170px;
    background: rgba(158,158,158,.2);
    border: none;
    margin-left: 0px;
    text-align: center !important;
    vertical-align: middle;
    line-height: 36px;
    padding-right: unset;
    padding: 0px 16px;
    display:inline;
}
#collapse4 .mdl-cell--12-col:nth-of-type(2), #collapse4 .mdl-cell--12-col:nth-of-type(3) {
    width: 50% !important;
    display:inline !important;
}
#collapse4 [name*="refreshDevices"] span, #collapse4 [name*="resultPage"] span {
    font-weight: 500;
    text-align: center !important;
    white-space: nowrap !important;
}
#collapse4 [name*="refreshDevices"]::before, #collapse4 [name*="resultPage"]::before {
    content: "";
}

@media (min-width: 840px)
.mdl-cell--8-col, .mdl-cell--8-col-desktop.mdl-cell--8-col-desktop {
    width: calc(76.6666666667% - 16px);
}'''
if(btnDone == true) {
    css += '''
button#btnNext {
    font-size: 0;
    display: flex;
}
button#btnNext::after {
    content: "Done";
    font-size: 14px;
}'''
}
css += '</style>'

return css
}

 /**
 * --END-- APP CSS METHODS (helpers-app-css)
 */