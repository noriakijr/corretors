import { StatusBar } from 'expo-status-bar';
import { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import {
  createPlano,
  createTitular,
  listPlanos,
  listTitulares,
  apiBase,
  type Plano,
  type Titular,
} from './src/api';

export default function App() {
  const [planos, setPlanos] = useState<Plano[]>([]);
  const [titulares, setTitulares] = useState<Titular[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [planoNome, setPlanoNome] = useState('');
  const [planoDescricao, setPlanoDescricao] = useState('');
  const [titularNome, setTitularNome] = useState('');
  const [titularCpf, setTitularCpf] = useState('');
  const [titularCelular, setTitularCelular] = useState('');
  const [titularCidade, setTitularCidade] = useState('');

  const load = useCallback(async () => {
    setError(null);
    try {
      const [nextPlanos, nextTitulares] = await Promise.all([listPlanos(), listTitulares()]);
      setPlanos(nextPlanos);
      setTitulares(nextTitulares);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Falha ao falar com a API');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function onCreatePlano() {
    const nome = planoNome.trim();
    if (!nome) {
      setError('Informe o nome do plano.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await createPlano({
        nome,
        descricao: planoDescricao.trim() || undefined,
      });
      setPlanoNome('');
      setPlanoDescricao('');
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Falha ao criar plano');
    } finally {
      setSaving(false);
    }
  }

  async function onCreateTitular() {
    const nome = titularNome.trim();
    if (!nome) {
      setError('Informe o nome do titular.');
      return;
    }
    const celular = titularCelular.trim();
    const cidade = titularCidade.trim();
    setSaving(true);
    setError(null);
    try {
      await createTitular({
        nome,
        cpf: titularCpf.trim() || undefined,
        contato:
          celular || cidade
            ? {
                celular: celular || undefined,
                endereco: cidade ? { cidade } : undefined,
              }
            : undefined,
      });
      setTitularNome('');
      setTitularCpf('');
      setTitularCelular('');
      setTitularCidade('');
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Falha ao criar titular');
    } finally {
      setSaving(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.kicker}>Corretors</Text>
        <Text style={styles.title}>Planos e titulares</Text>
        <Text style={styles.api}>{apiBase()}</Text>

        {loading ? <ActivityIndicator color="#1f4b3a" style={styles.spinner} /> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}

        <Text style={styles.section}>Planos</Text>
        {planos.length === 0 && !loading ? <Text style={styles.empty}>Nenhum plano cadastrado.</Text> : null}
        {planos.map((plano) => (
          <View key={plano.id} style={styles.card}>
            <Text style={styles.cardTitle}>{plano.nome}</Text>
            {plano.descricao ? <Text style={styles.cardBody}>{plano.descricao}</Text> : null}
          </View>
        ))}
        <TextInput
          style={styles.input}
          value={planoNome}
          onChangeText={setPlanoNome}
          placeholder="Nome do plano"
          placeholderTextColor="#8a8175"
        />
        <TextInput
          style={styles.input}
          value={planoDescricao}
          onChangeText={setPlanoDescricao}
          placeholder="Descrição"
          placeholderTextColor="#8a8175"
        />
        <Pressable
          style={({ pressed }) => [styles.button, pressed && styles.buttonPressed, saving && styles.buttonDisabled]}
          onPress={() => void onCreatePlano()}
          disabled={saving}
        >
          <Text style={styles.buttonText}>Adicionar plano</Text>
        </Pressable>

        <Text style={styles.section}>Titulares</Text>
        {titulares.length === 0 && !loading ? (
          <Text style={styles.empty}>Nenhum titular cadastrado.</Text>
        ) : null}
        {titulares.map((titular) => (
          <View key={titular.id} style={styles.card}>
            <Text style={styles.cardTitle}>{titular.nome}</Text>
            {titular.cpf ? <Text style={styles.cardBody}>CPF {titular.cpf}</Text> : null}
            {titular.contato?.celular ? (
              <Text style={styles.cardBody}>{titular.contato.celular}</Text>
            ) : null}
            {titular.contato?.endereco?.cidade ? (
              <Text style={styles.cardBody}>{titular.contato.endereco.cidade}</Text>
            ) : null}
          </View>
        ))}
        <TextInput
          style={styles.input}
          value={titularNome}
          onChangeText={setTitularNome}
          placeholder="Nome do titular"
          placeholderTextColor="#8a8175"
        />
        <TextInput
          style={styles.input}
          value={titularCpf}
          onChangeText={setTitularCpf}
          placeholder="CPF"
          placeholderTextColor="#8a8175"
        />
        <TextInput
          style={styles.input}
          value={titularCelular}
          onChangeText={setTitularCelular}
          placeholder="Celular"
          placeholderTextColor="#8a8175"
          keyboardType="phone-pad"
        />
        <TextInput
          style={styles.input}
          value={titularCidade}
          onChangeText={setTitularCidade}
          placeholder="Cidade"
          placeholderTextColor="#8a8175"
        />
        <Pressable
          style={({ pressed }) => [styles.button, pressed && styles.buttonPressed, saving && styles.buttonDisabled]}
          onPress={() => void onCreateTitular()}
          disabled={saving}
        >
          <Text style={styles.buttonText}>Adicionar titular</Text>
        </Pressable>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#f4f1ea',
  },
  content: {
    padding: 20,
    paddingTop: 56,
    paddingBottom: 48,
  },
  kicker: {
    color: '#1f4b3a',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  title: {
    color: '#1c2430',
    fontSize: 28,
    fontWeight: '700',
    marginTop: 4,
  },
  api: {
    color: '#6e675e',
    fontSize: 13,
    marginTop: 6,
    marginBottom: 12,
  },
  spinner: {
    marginVertical: 8,
  },
  error: {
    backgroundColor: '#f8e6e1',
    color: '#8a2e1b',
    padding: 12,
    borderRadius: 8,
    marginBottom: 12,
  },
  section: {
    color: '#1c2430',
    fontSize: 20,
    fontWeight: '700',
    marginTop: 18,
    marginBottom: 8,
  },
  empty: {
    color: '#6e675e',
    marginBottom: 8,
  },
  card: {
    backgroundColor: '#fffdf8',
    borderRadius: 10,
    padding: 12,
    marginBottom: 8,
  },
  cardTitle: {
    color: '#1c2430',
    fontSize: 16,
    fontWeight: '600',
  },
  cardBody: {
    color: '#4d463e',
    marginTop: 2,
  },
  input: {
    backgroundColor: '#fffdf8',
    borderColor: '#ddd4c6',
    borderRadius: 8,
    borderWidth: 1,
    color: '#1c2430',
    marginBottom: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  button: {
    alignItems: 'center',
    backgroundColor: '#1f4b3a',
    borderRadius: 8,
    marginTop: 4,
    paddingVertical: 12,
  },
  buttonPressed: {
    backgroundColor: '#16382b',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#f4f1ea',
    fontSize: 16,
    fontWeight: '700',
  },
});
