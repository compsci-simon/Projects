import { width } from "dom-helpers"

const list = []

for (let i = 0; i < 250; i++) {
  let number = Math.max(Math.floor(Math.random() * 500), 5)
  list.push(number)
}

const Graph = () => {
  
  return (
    <div className="graph">
      { list.map((height) => {
        return <div style={ {
          margin: 'auto 1px auto 1px',
          width: '2px',
          height: height,
          backgroundColor: '#F4C430',
          marginBottom: '0'
        } }></div>
      }) }
    </div>
  )


}

export default Graph
