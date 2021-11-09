import Container from 'react-bootstrap/Container';
import Navbar from 'react-bootstrap/Navbar';
import Nav from 'react-bootstrap/Nav';

function NavbarComponent() {
  return (
    <div>
        <Navbar bg="yellow" variant="light">
          <Container>
          <Navbar.Brand href="#home">AlgoViz</Navbar.Brand>
          <Nav className="me-auto">
            <Nav.Link href="#home">Sorting algorithms</Nav.Link>
          </Nav>
          </Container>
        </Navbar>
    </div>
  )
}

export default NavbarComponent
