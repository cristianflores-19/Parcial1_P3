package Ejercicios;

public class Ejercicio1 {

	public static void main(String[] args) {
		
        int[] ejemplo1 = {1, 2, 3, 4, 5};
        int[] ejemplo2 = {17, 19, 21};
        int[] ejemplo3 = {5, 5, 5};

        
        System.out.println("Entrada [1, 2, 3, 4, 5]  Salida: " + score(ejemplo1)); 
        System.out.println("Entrada [17, 19, 21]     Salida: " + score(ejemplo2)); 
        System.out.println("Entrada [5, 5, 5]        Salida: " + score(ejemplo3)); 

	}
	public static int score(int[] numbers) {
        int puntajeTotal = 0;
        
       
        if (numbers == null || numbers.length == 0) {
            return 0;
        }

        
        for (int i = 0; i < numbers.length; i++) {
            int num = numbers[i];

            if (num == 5) {
                
                puntajeTotal += 5;
            } else if (num % 2 == 0) {
                puntajeTotal += 1;
            } else {
                puntajeTotal += 3;
            }
        }

        return puntajeTotal;
	
	}
}
