import { Component } from "react";
import './GraphVisualizer.css'
import Button from '@mui/material/Button';
import { Slider } from "@mui/material";
import { insertionSortAnimation, selectionSort } from "../sortingAlgorithms/sortingAlgorithms";

var NUM_ITEMS = 50;
let MAX_BAR_HEIGHT = 600;

class GraphVisualizer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      list: []
    }
  }

  resetArray() {
    var list = []
    for (let i = 0; i < NUM_ITEMS; i++) {
      const num = Math.max(5, Math.floor(Math.random() * MAX_BAR_HEIGHT))
      list.push(num)
    }
    this.setState({
      list: list,
    });
  }

  sort = async (type) => {
    var list = this.state.list
    var compare = 0
    var swap = 0
    var animationDetail = null;
    if (type === "insertion") {
      animationDetail = insertionSortAnimation(list);
      console.log('here');
    } else if (type === "selection") {
      animationDetail = selectionSort(list);
    }
    let animations = animationDetail['animations'];
    let duration = animationDetail['duration'];
    
    let compares = document.getElementById('compares');
    let swaps = document.getElementById('swaps');
    console.log(animationDetail['animations'])
    
    for (const animation of animations) {
      if (animation['compare']) {
        let id0 = animation['compare'][0]
        let id1 = animation['compare'][1]
        document.getElementById(id0).classList.add('comparison')
        document.getElementById(id1).classList.add('comparison')
        compare++
        compares.innerHTML = `Compares: ${compare}`;
        await new Promise(resolve => setTimeout(resolve, duration));
        document.getElementById(id0).classList.remove('comparison')
        document.getElementById(id1).classList.remove('comparison')
      } else if (animation['swap']) {
        let id0 = animation['swap'][0][0]
        let id1 = animation['swap'][1][0]
        let height0 = animation['swap'][0][1]
        let height1 = animation['swap'][1][1]
        document.getElementById(id0).style.height = `${height0}px`;
        document.getElementById(id1).style.height = `${height1}px`;
        swap++
        swaps.innerHTML = `Swaps: ${swap}`;
        await new Promise(resolve => setTimeout(resolve, duration));
      } else {
        console.log(animation)
      }
    }
  }

  componentDidMount() {
    this.resetArray();
  }

  render () {
    const {list} = this.state

    return (
      <div>
      <Button onClick={() => this.resetArray()}>Reset Graph</Button>
      <Button onClick={() => this.sort('insertion')}>Insertion sort</Button>
      <Button onClick={() => this.sort('selection')}>Selection sort</Button>
      <Slider 
        min={4} 
        max={100} 
        defaultValue={NUM_ITEMS} 
        aria-label="Default" 
        valueLabelDisplay="auto" 
        onChange={(e, val) => {
          NUM_ITEMS = val;
          this.resetArray();
        }}
      />
      <hr />
        
        <div className="array-container" style={{height: MAX_BAR_HEIGHT}}>
          {list.map((height, index) => (
            <div 
              className="bar" 
              key={index} 
              id={index}
              style={{
                height: `${height}px`,
                width: `calc(500px/${NUM_ITEMS})`
              }}
            ></div>
          ))}
        </div>
        <div className="d-flex mt-3 justify-content-md-around">
          <h2 id="compares" className="border border-2 p-2">Compares: 0</h2>
          <h2 id="swaps" className="border border-2 p-2">Swaps: 0</h2>
        </div>
      </div>
    );
  }
}

export default GraphVisualizer;