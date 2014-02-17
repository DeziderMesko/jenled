import jssc.*
import jssc.SerialPort.*
//     Unstable, Failure, Success, Start
// Un    blink    fade     fade     fade2b
// Fa    fade     blink    fade     fade2b
// Su    fade     fade     blink    fade2b


enum State {
    Unstable("255,120,10"), Failure("255,0,0"), Success("0,255,0"), Start("0,0,255"), Undef("255,255,255");
    
    def String rgb;
    State(String rgbColor){
        rgb = rgbColor
    }
}

class LedControler{
    def BLINK = "S,1\n";
    
    def serialPort = new SerialPort("COM5");
    def blinkInterval = 300;
    def fadeInterval = 1500;
    
    def initialize(){
        serialPort.openPort();
        serialPort.setParams(9600, 8, 1, 0, false, false)
    }

    def destroy(){
        serialPort.closePort()
    }

    def fade2black(State state){
        fade2black(state, 2)
    }
    
    def fade2black(State state, int count){
        for (i in 1..count){
            serialPort.writeBytes("F,1,0,0,0\n".getBytes())
            Thread.sleep(fadeInterval);    
            serialPort.writeBytes("F,1,${state.rgb}\n".getBytes())
            Thread.sleep(fadeInterval);
        }
    }
    
    def fade(State toState){
        serialPort.writeBytes("F,1,${toState.rgb}\n".getBytes())
    
    }
    def blink(){
        blink(3)
    }
    
    def blink(int count){
        for (i in 1..count){
            Thread.sleep(blinkInterval);    
            serialPort.writeBytes(BLINK.getBytes())
            Thread.sleep(blinkInterval);    
            serialPort.writeBytes(BLINK.getBytes())
        }
    }
}

class StateMachine {
    State currentState = State.Undef
    LedControler ledControler
    def StateMachine(LedControler ledControler){
        this.ledControler = ledControler
    }
    
    def processState(State nextState){
        if(nextState.equals(currentState)){
            ledControler.blink()
        } else if(nextState.equals(State.Start)){
            ledControler.fade2black(currentState)
        } else if(!nextState.equals(currentState)){
            currentState = nextState
            ledControler.fade(nextState)
        } 
    }
    def destroy(){
        ledControler.destroy()
    }
}

import java.net.ServerSocket
import groovy.json.JsonSlurper

class TcpServer {
    def StateMachine stateMachine
    def TcpServer(StateMachine stateMachine){
        this.stateMachine = stateMachine
    }

    def start(){
        def server = new ServerSocket(4444)
        def slurper = new JsonSlurper()
        def shouldBreak = false
        while(true) {
            print "Waiting for connection... "
            server.accept { socket ->
                println "connected"     
                socket.withStreams { input, output ->
                    def reader = input.newReader()
                    def buffer = reader.readLine()
                    if(buffer.startsWith("q")){
                        shouldBreak = true
                        return
                    }
                    def result = slurper.parseText(buffer)
                    println "$result.build.number: $result.name, $result.build.phase -> $result.build.status"
                    if(!result.name.contains("Light")){
                        return
                    }
                    if(result.build.phase.equals("STARTED")){
                        stateMachine.processState(State.Start)
                        return                    
                    }
                    if(result.build.phase.equals("FINISHED")){
                        switch(result.build.status){
                        case 'SUCCESS':
                            stateMachine.processState(State.Success)
                            break;
                        case 'UNSTABLE':                        
                            stateMachine.processState(State.Unstable)
                            break;
                        case 'FAILURE':
                            stateMachine.processState(State.Failure)
                            break;
                        default:
                            stateMachine.processState(State.Undef)
                        }
                    }
                }
                socket.close()
            }
            if(shouldBreak){
                server.close()
                break
            }
        }
        stateMachine.destroy()    
        server.close()
    }
}

def lc = new LedControler()
lc.initialize()
def sm = new StateMachine(lc)
def ts = new TcpServer(sm)
ts.start()

return