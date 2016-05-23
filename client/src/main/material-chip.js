import React from 'react'

export default class MaterialChip extends React.Component{
	constructor(props){
		super(props);
	};



	render(){
	    const chipStyle={
          backgroundColor: '#1B7E5A'/*'#e5e5e5'*/,
          WebkitBorderRadius: '10px 30% 30% 30%',
          MozBorderRadius: '10px 30% 10% 10%',
          OBorderRadius: '10px 30px 10% 10%',
          borderRadius: '12px 12px 12px 12px',
          position: 'relative',
          display: 'flex',
          flexDirection: 'row',
          textAlign: 'center',
          height: '32px',
          lineHeight: '32px',
          margin: '2px 0px',
          minWidth: '50px'
        };
        const chipNameStyle={
          WebkitUserSelect: 'none',
          maxWidth: '150px',
          overflow: 'hidden',
          textOverflow: 'hidden',
          whiteSpace: 'nowrap',
          margin:'0px auto',
          color:'#FFFFFF',
          fontFamily:'Roboto'

        };
		return (
			React.createElement("div",{key:1,style:chipStyle},[
				React.createElement("span",{key:1,style:chipNameStyle},this.props.text)
			])
		);
	};

}