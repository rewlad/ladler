import React from 'react'
import IconNavigationClose from 'material-ui/svg-icons/navigation/close'
export default class MaterialChip extends React.Component{
	constructor(props){
		super(props);
	};



	render(){
	    const chipStyle={
          backgroundColor: '#e4e4e4',
          WebkitBorderRadius: '16px',
          MozBorderRadius: '16px',
          OBorderRadius: '16px',
          borderRadius: '16px',
          display: 'inline-block',
          height: '32px',
          lineHeight: '32px',
          padding: '0 12px',
          fontSize: '13px',
          fontWeight: '500',
          whiteSpace:"nowrap",
          width:"100%",
          boxSizing:"border-box",

        };
        const closeIconStyle={
            cursor:"pointer",
            //float:"right",
            height:"16px",
            width:"16px",
            lineHeight:"32px",
            paddingLeft:"8px",
            display:"inline-block",
            //whiteSpace:"nowrap",
            verticalAlign:"text-bottom"

        }
        const imgStyle={
            float:"left",
            margin:"0 8px 0 -12px",
            height: "32px",
            width:"32px",
            borderRadius:"50%",
            border:"0",
            lineHeight:"32px"
        }
        const textStyle={
            whiteSpace:"nowrap",
            display:"inline-block",
            overflow:"hidden",
            textOverflow:"ellipsis",
            width:"calc(100%"+ (this.props.children?" - 32px":"") + (this.props.onClick?" - 16px":"")+")",
            textAlign:"center"
        }
        const img=this.props.children?React.createElement("div",{key:"img",style:imgStyle},this.props.children):null
        const closeIcon=this.props.onClick?React.createElement("div",{key:"close",style:{display:"inline-block",float:"right"},
            onClick:this.props.onClick},React.createElement(IconNavigationClose,{key:"close",style:closeIconStyle})):null
		return (
			React.createElement("div",{key:1,style:chipStyle},[
			    img,closeIcon,
				React.createElement("div",{key:1,style:textStyle},this.props.text)

			])
		);
	};

}