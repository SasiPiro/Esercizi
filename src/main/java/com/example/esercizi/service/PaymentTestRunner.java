package com.example.esercizi.service;

import com.example.esercizi.dto.CalculationResultDto;
import com.example.esercizi.model.Session;
import com.example.esercizi.model.User;
import com.example.esercizi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentTestRunner implements CommandLineRunner {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private CartService cartService;

    @Override
    public void run(String... args) {
        try {
            System.out.println("--- INIZIO TEST PAGAMENTO ---");

            // 1. Recuperiamo l'utente ID 1 dal DB
            User user = userRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Utente 1 non trovato nel DB!"));

            // 2. Creiamo una sessione (o ne recuperiamo una esistente)
            // Possiamo passare null per farne creare una nuova dal tuo service


            // IMPORTANTE: Il metodo che vuoi chiamare recupera il carrello dal DB.
            // Se il carrello è vuoto, il tuo codice lancia CartEmptyException.
            // Quindi aggiungiamo un prodotto prima di calcolare.
            cartService.addItem(null, user, "P004", 2); // Esempio: Prodotto ID 101, Qta 2

            // 3. CHIAMATA AL METODO CHE VOLEVI
            String coupon = "SAVE10";
            CalculationResultDto finale = paymentService.calculateTotal(null, user, coupon);

            System.out.println("RISULTATO:");
            System.out.println("Utente: " + user.getUsername() + " (VIP: " + user.isVip() + ")");
            System.out.println("Coupon usato: " + coupon);
            System.out.println("TOTALE DA PAGARE: €" + finale);

        } catch (Exception e) {
            System.err.println("ERRORE DURANTE IL TEST: " + e.getMessage());
        }
    }
}