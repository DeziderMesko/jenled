const int ledCount = 6;

//rgb,rgb
int ledPin[ledCount] = {
  3,6,5,9,10,11};
int ledState[ledCount] = {
  1,1,1,1,1,1};
int ledInterLevel[ledCount] = {
};
int ledLevel[ledCount] = {
};

void turnLedOff(int led){
  ledState[led]=0;
  digitalWrite(ledPin[led], LOW);
}

void turnLedOn(int led){
  ledState[led]=1;
  analogWrite(ledPin[led], ledInterLevel[led]);
}

void switchLed(int led){
  if(ledState[led]==0){
    turnLedOn(led);
  } 
  else {
    turnLedOff(led);
  }
}


void setup() {
  Serial.begin(9600, SERIAL_8N1);
  Serial.setTimeout(100);
  for(int led = 0; led < 3; led++){ //ledCount
    pinMode(ledPin[led], OUTPUT);     
  }
}

void loop() {
  int next = 0;
  while (Serial.available() > 0) {
    next = Serial.peek();
    switch(next){
    case 's':
    case 'S':
      switchRGBFn();
      break;
    case 'f':
    case 'F':
      setRGBForFade();
      break;
    case 13:
      Serial.read();
      break;
    default:
      Serial.println("S-witch (LED) => S,1");
      Serial.println("F-ade (LED, RGB) => F,1,242,200,53");
      Serial.print("Unknown character #: ");
      Serial.println(Serial.read());
    }
  }
  performFade();
}

void switchRGBFn(){
  Serial.read();
  int led = Serial.parseInt();

  switchLed(led+0-1);
  switchLed(led+1-1);
  switchLed(led+2-1);
}

void setRGBForFade(){
  Serial.read();
  int led = Serial.parseInt();  
  int r = Serial.parseInt();
  int g = Serial.parseInt();
  int b = Serial.parseInt();

  ledLevel[led+0-1]=r;
  ledLevel[led+1-1]=g;
  ledLevel[led+2-1]=b;  
}

void performFade(){
  for(int i = 0; i < ledCount; i++){
    if((ledInterLevel[i]==ledLevel[i]) || ledState[i]==0){
      continue;
    }
    if((ledInterLevel[i]-ledLevel[i])>0){
      ledInterLevel[i]--;
    }
    else {
      ledInterLevel[i]++;
    }
    analogWrite(ledPin[i], ledInterLevel[i]);
  }
  delay(6); 
}











