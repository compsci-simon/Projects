import Header from "./components/Header";
import Navbar from "./components/NavbarComponent";
import Graph from "./components/Graph";
import 'bootstrap/dist/css/bootstrap.css';
import './App.css';

function App() {
  return (
    <div className="App">
      < Navbar/>
      <div className="container body">
        <div className="centerVert">
          < Graph/>
        </div>
      </div>
    </div>
  );
}

export default App;
